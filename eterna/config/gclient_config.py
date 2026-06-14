# Eterna Browser gclient configuration
  # Copy as .gclient to your workspace root, then: gclient sync

  solutions = [
    {
      "name"     : "src",
      "url"      : "https://github.com/ferelking242/eterna-browser.git",
      "managed"  : False,
      "custom_deps": {},
      "custom_vars": {
        "checkout_nacl": False,
        "checkout_pgo_profiles": False,
      },
    },
  ]

  target_os  = ["android"]
  target_cpu = ["arm64"]
  