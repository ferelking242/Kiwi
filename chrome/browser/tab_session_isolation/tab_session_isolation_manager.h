// Copyright 2024 Kiwi Browser Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license.

#ifndef CHROME_BROWSER_TAB_SESSION_ISOLATION_TAB_SESSION_ISOLATION_MANAGER_H_
#define CHROME_BROWSER_TAB_SESSION_ISOLATION_TAB_SESSION_ISOLATION_MANAGER_H_

#include <map>
#include <string>

#include "base/memory/singleton.h"
#include "base/synchronization/lock.h"
#include "content/public/browser/storage_partition_config.h"

namespace content {
class BrowserContext;
class SiteInstance;
}  // namespace content

// ----------------------------------------------------------------------------
// TabSessionIsolationManager
//
// Manages isolated storage partitions for Kiwi "Isolated Tab" sessions.
//
// Each isolated tab is assigned a unique isolation_key (UUID string).
// This key maps to a StoragePartitionConfig that gives the tab its own:
//   - Cookie store
//   - localStorage / sessionStorage
//   - Cache
//   - IndexedDB
//   - Service Worker registrations
//
// Storage lives under:
//   <profile>/Storage/ext/kiwi_tab_isolation/<hash(isolation_key)>/
//
// When a tab is closed, DestroyIsolationSession() should be called to
// schedule asynchronous deletion of all on-disk storage for that session.
//
// Maximum number of concurrent isolated sessions: kMaxIsolatedSessions.
// ----------------------------------------------------------------------------
class TabSessionIsolationManager {
 public:
  // Domain used for all tab-isolated storage partitions.
  // Must not conflict with extension IDs (those are hex strings).
  static constexpr char kIsolationDomain[] = "kiwi_tab_isolation";

  // Hard limit on concurrent isolated sessions to protect RAM / disk.
  static constexpr int kMaxIsolatedSessions = 10;

  static TabSessionIsolationManager* GetInstance();

  TabSessionIsolationManager(const TabSessionIsolationManager&) = delete;
  TabSessionIsolationManager& operator=(const TabSessionIsolationManager&) =
      delete;

  // Returns true if a new isolated session can be created (i.e. we haven't
  // hit kMaxIsolatedSessions yet).
  bool CanCreateIsolatedSession() const;

  // Returns the number of currently active isolated sessions.
  int GetActiveSessionCount() const;

  // Creates a new isolated session and returns its isolation_key.
  // Returns empty string if kMaxIsolatedSessions is reached.
  // Thread-safe.
  std::string CreateIsolationSession(content::BrowserContext* browser_context);

  // Returns the StoragePartitionConfig for the given isolation_key, or the
  // default config if the key is unknown.
  content::StoragePartitionConfig GetPartitionConfig(
      content::BrowserContext* browser_context,
      const std::string& isolation_key) const;

  // Creates and returns a SiteInstance that uses the isolated partition
  // for isolation_key. This SiteInstance should be passed to
  // WebContents::CreateParams when creating the isolated tab's WebContents.
  //
  // Returns nullptr if isolation_key is unknown.
  scoped_refptr<content::SiteInstance> CreateSiteInstanceForIsolatedTab(
      content::BrowserContext* browser_context,
      const std::string& isolation_key);

  // Schedules async deletion of all on-disk storage for isolation_key.
  // Call this when the corresponding tab is destroyed.
  // Thread-safe.
  void DestroyIsolationSession(content::BrowserContext* browser_context,
                               const std::string& isolation_key);

  // Returns true if the given isolation_key corresponds to an active session.
  bool IsActiveSession(const std::string& isolation_key) const;

 private:
  friend struct base::DefaultSingletonTraits<TabSessionIsolationManager>;

  TabSessionIsolationManager();
  ~TabSessionIsolationManager();

  // Generates a cryptographically random isolation key (UUID).
  static std::string GenerateIsolationKey();

  mutable base::Lock lock_;

  // Maps isolation_key -> StoragePartitionConfig.
  std::map<std::string, content::StoragePartitionConfig> active_sessions_;
};

#endif  // CHROME_BROWSER_TAB_SESSION_ISOLATION_TAB_SESSION_ISOLATION_MANAGER_H_
