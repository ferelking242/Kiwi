// Copyright 2024 Kiwi Browser Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license.

#include "chrome/browser/tab_session_isolation/tab_session_isolation_manager.h"

#include "base/logging.h"
#include "base/memory/singleton.h"
#include "base/synchronization/lock.h"
#include "base/unguessable_token.h"
#include "content/public/browser/browser_context.h"
#include "content/public/browser/browser_thread.h"
#include "content/public/browser/site_instance.h"
#include "content/public/browser/storage_partition_config.h"

// static
constexpr char TabSessionIsolationManager::kIsolationDomain[];
constexpr int TabSessionIsolationManager::kMaxIsolatedSessions;

// static
TabSessionIsolationManager* TabSessionIsolationManager::GetInstance() {
  return base::Singleton<TabSessionIsolationManager>::get();
}

TabSessionIsolationManager::TabSessionIsolationManager() = default;
TabSessionIsolationManager::~TabSessionIsolationManager() = default;

bool TabSessionIsolationManager::CanCreateIsolatedSession() const {
  base::AutoLock lock(lock_);
  return static_cast<int>(active_sessions_.size()) < kMaxIsolatedSessions;
}

int TabSessionIsolationManager::GetActiveSessionCount() const {
  base::AutoLock lock(lock_);
  return static_cast<int>(active_sessions_.size());
}

std::string TabSessionIsolationManager::CreateIsolationSession(
    content::BrowserContext* browser_context) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  DCHECK(browser_context);

  base::AutoLock lock(lock_);

  if (static_cast<int>(active_sessions_.size()) >= kMaxIsolatedSessions) {
    LOG(WARNING) << "[TabSessionIsolation] Cannot create isolated session: "
                 << "maximum of " << kMaxIsolatedSessions << " reached.";
    return std::string();
  }

  std::string isolation_key = GenerateIsolationKey();

  // Create a unique, persistent StoragePartitionConfig for this session.
  // partition_domain = kIsolationDomain (constant, identifies Kiwi isolation)
  // partition_name   = isolation_key    (unique UUID per session)
  // in_memory        = false            (persist through app background/restore)
  content::StoragePartitionConfig config =
      content::StoragePartitionConfig::Create(
          browser_context,
          kIsolationDomain,
          isolation_key,
          /*in_memory=*/false);

  active_sessions_[isolation_key] = config;

  VLOG(1) << "[TabSessionIsolation] Created isolated session: "
          << isolation_key
          << " (total active: " << active_sessions_.size() << ")";

  return isolation_key;
}

content::StoragePartitionConfig
TabSessionIsolationManager::GetPartitionConfig(
    content::BrowserContext* browser_context,
    const std::string& isolation_key) const {
  base::AutoLock lock(lock_);

  auto it = active_sessions_.find(isolation_key);
  if (it == active_sessions_.end()) {
    LOG(WARNING) << "[TabSessionIsolation] Unknown isolation_key: "
                 << isolation_key << ". Returning default partition.";
    return content::StoragePartitionConfig::CreateDefault(browser_context);
  }

  return it->second;
}

scoped_refptr<content::SiteInstance>
TabSessionIsolationManager::CreateSiteInstanceForIsolatedTab(
    content::BrowserContext* browser_context,
    const std::string& isolation_key) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);

  content::StoragePartitionConfig config =
      GetPartitionConfig(browser_context, isolation_key);

  // SiteInstance::CreateForGuest() creates a SiteInstance that:
  //   1. Has its own process group (process isolation)
  //   2. Uses the specified StoragePartitionConfig (storage isolation)
  // This is the same mechanism used by Chrome's <webview> element.
  return content::SiteInstance::CreateForGuest(browser_context, config);
}

void TabSessionIsolationManager::DestroyIsolationSession(
    content::BrowserContext* browser_context,
    const std::string& isolation_key) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  DCHECK(browser_context);

  content::StoragePartitionConfig config;
  {
    base::AutoLock lock(lock_);
    auto it = active_sessions_.find(isolation_key);
    if (it == active_sessions_.end()) {
      VLOG(1) << "[TabSessionIsolation] DestroyIsolationSession: key not found: "
              << isolation_key;
      return;
    }
    config = it->second;
    active_sessions_.erase(it);
  }

  VLOG(1) << "[TabSessionIsolation] Destroying isolated session: "
          << isolation_key;

  // Schedule asynchronous obliteration of all on-disk storage for this
  // partition. This deletes cookies, IndexedDB, localStorage, cache, etc.
  //
  // We use the partition_domain as the key. The partition_name (isolation_key)
  // is embedded in the directory path via its SHA256 hash, so obliterating
  // the partition_domain + partition_name combo cleans up correctly.
  //
  // See StoragePartitionImplMap::AsyncObliterate() for implementation details.
  browser_context->AsyncObliterateStoragePartition(
      kIsolationDomain + std::string("_") + isolation_key,
      /*on_gc_required=*/base::DoNothing(),
      /*done_callback=*/base::BindOnce([](const std::string& key) {
        VLOG(1) << "[TabSessionIsolation] Storage obliterated for: " << key;
      }, isolation_key));
}

bool TabSessionIsolationManager::IsActiveSession(
    const std::string& isolation_key) const {
  base::AutoLock lock(lock_);
  return active_sessions_.count(isolation_key) > 0;
}

// static
std::string TabSessionIsolationManager::GenerateIsolationKey() {
  // UnguessableToken gives us 128 bits of cryptographic randomness.
  return base::UnguessableToken::Create().ToString();
}
