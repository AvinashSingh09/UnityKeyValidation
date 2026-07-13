using System;
using System.Security.Cryptography;
using System.Text;
using UnityEngine;

namespace KeyVault.SDK
{
    /// <summary>
    /// Generates a unique hardware fingerprint for the current machine.
    /// Combines multiple system identifiers to create a hash that is
    /// difficult to spoof.
    /// </summary>
    public static class HardwareFingerprint
    {
        /// <summary>
        /// Returns a SHA256 hash of combined hardware identifiers.
        /// </summary>
        public static string Generate()
        {
            string rawId = GatherSystemInfo();
            return ComputeSha256Hash(rawId);
        }

        /// <summary>
        /// Returns a human-readable machine name.
        /// </summary>
        public static string GetMachineName()
        {
            return SystemInfo.deviceName;
        }

        private static string GatherSystemInfo()
        {
            StringBuilder sb = new StringBuilder();

            // Device unique identifier (hardware-based on most platforms)
            sb.Append(SystemInfo.deviceUniqueIdentifier);
            sb.Append("|");

            // Processor information
            sb.Append(SystemInfo.processorType);
            sb.Append("|");
            sb.Append(SystemInfo.processorCount);
            sb.Append("|");

            // Graphics card
            sb.Append(SystemInfo.graphicsDeviceName);
            sb.Append("|");
            sb.Append(SystemInfo.graphicsDeviceVendor);
            sb.Append("|");

            // OS
            sb.Append(SystemInfo.operatingSystem);
            sb.Append("|");

            // Device name
            sb.Append(SystemInfo.deviceName);

            return sb.ToString();
        }

        private static string ComputeSha256Hash(string rawData)
        {
            using (SHA256 sha256 = SHA256.Create())
            {
                byte[] bytes = sha256.ComputeHash(Encoding.UTF8.GetBytes(rawData));
                StringBuilder builder = new StringBuilder();
                foreach (byte b in bytes)
                {
                    builder.Append(b.ToString("x2"));
                }
                return builder.ToString();
            }
        }
    }
}
