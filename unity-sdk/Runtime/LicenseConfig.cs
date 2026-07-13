using System;

namespace KeyVault.SDK
{
    /// <summary>
    /// ScriptableObject-like configuration for the license validation server.
    /// In Unity, extend this from ScriptableObject. For standalone use, create an instance and configure.
    /// </summary>
    [Serializable]
    public class LicenseConfig
    {
        /// <summary>
        /// The base URL of your KeyVault API server (e.g., https://your-api.onrender.com/api)
        /// </summary>
        public string serverUrl = "http://localhost:8080/api";

        /// <summary>
        /// The product code assigned in the KeyVault dashboard (e.g., STEELVR-001)
        /// </summary>
        public string productCode = "";

        /// <summary>
        /// How often (in seconds) the heartbeat check runs during the session.
        /// Set to 0 to disable periodic checks.
        /// </summary>
        public float heartbeatIntervalSeconds = 300f; // 5 minutes

        /// <summary>
        /// If true, shows the built-in activation UI when no valid license is found.
        /// </summary>
        public bool showActivationUI = true;
    }
}
