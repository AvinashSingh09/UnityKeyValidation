using System;
using System.Collections;
using System.Text;
using UnityEngine;
using UnityEngine.Networking;

namespace KeyVault.SDK
{
    /// <summary>
    /// Main license validation component. Attach to a persistent GameObject in your scene.
    /// Handles activation, periodic heartbeat, and deactivation of license keys.
    /// 
    /// Usage:
    ///   1. Create a LicenseConfig and assign your server URL + product code
    ///   2. Attach KeyValidator to a GameObject
    ///   3. Call KeyValidator.Instance.Activate(key, onResult) at app startup
    ///   4. KeyValidator automatically runs heartbeat checks in the background
    /// </summary>
    public class KeyValidator : MonoBehaviour
    {
        public static KeyValidator Instance { get; private set; }

        [Header("Configuration")]
        public LicenseConfig config = new LicenseConfig();

        [Header("State")]
        [SerializeField] private bool _isLicensed = false;
        [SerializeField] private string _currentKey = "";
        [SerializeField] private string _licenseStatus = "Not Validated";

        /// <summary>True if the current license is valid and active.</summary>
        public bool IsLicensed => _isLicensed;

        /// <summary>Human-readable license status.</summary>
        public string LicenseStatus => _licenseStatus;

        /// <summary>Fires when license state changes (valid/invalid, reason).</summary>
        public event Action<bool, string> OnLicenseStateChanged;

        private string _hardwareId;
        private string _machineName;
        private Coroutine _heartbeatCoroutine;

        private const string LICENSE_KEY_PREF = "KV_LicenseKey";

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }

            Instance = this;
            DontDestroyOnLoad(gameObject);

            _hardwareId = HardwareFingerprint.Generate();
            _machineName = HardwareFingerprint.GetMachineName();

            // Try to restore saved key
            _currentKey = PlayerPrefs.GetString(LICENSE_KEY_PREF, "");
        }

        private void Start()
        {
            // If we have a saved key, try to validate it
            if (!string.IsNullOrEmpty(_currentKey))
            {
                Activate(_currentKey, null);
            }
        }

        /// <summary>
        /// Activate a license key on this machine.
        /// </summary>
        /// <param name="key">The license key (e.g., XXXXX-XXXXX-XXXXX-XXXXX)</param>
        /// <param name="callback">Called with (success, message) when activation completes</param>
        public void Activate(string key, Action<bool, string> callback)
        {
            _currentKey = key;
            PlayerPrefs.SetString(LICENSE_KEY_PREF, key);
            PlayerPrefs.Save();

            StartCoroutine(SendValidationRequest("/validate/activate", key, (response) =>
            {
                _isLicensed = response.valid;
                _licenseStatus = response.valid ? "Active" : response.reason;

                if (response.valid)
                {
                    Debug.Log($"[KeyVault] License activated successfully. Expires: {response.validUntil}");
                    StartHeartbeat();
                }
                else
                {
                    Debug.LogWarning($"[KeyVault] Activation failed: {response.reason}");
                    StopHeartbeat();
                }

                OnLicenseStateChanged?.Invoke(response.valid, response.reason);
                callback?.Invoke(response.valid, response.valid ? "License activated" : response.reason);
            }));
        }

        /// <summary>
        /// Manually trigger a validation check.
        /// </summary>
        public void ValidateNow(Action<bool, string> callback = null)
        {
            if (string.IsNullOrEmpty(_currentKey))
            {
                callback?.Invoke(false, "No license key set");
                return;
            }

            StartCoroutine(SendValidationRequest("/validate/check", _currentKey, (response) =>
            {
                bool wasLicensed = _isLicensed;
                _isLicensed = response.valid;
                _licenseStatus = response.valid ? "Active" : response.reason;

                if (wasLicensed && !response.valid)
                {
                    Debug.LogWarning($"[KeyVault] License invalidated: {response.reason}");
                    StopHeartbeat();
                    OnLicenseStateChanged?.Invoke(false, response.reason);
                }

                callback?.Invoke(response.valid, response.reason);
            }));
        }

        /// <summary>
        /// Deactivate the license from this machine (frees up an activation slot).
        /// </summary>
        public void Deactivate(Action<bool, string> callback = null)
        {
            if (string.IsNullOrEmpty(_currentKey))
            {
                callback?.Invoke(false, "No license key set");
                return;
            }

            StartCoroutine(SendValidationRequest("/validate/deactivate", _currentKey, (response) =>
            {
                _isLicensed = false;
                _licenseStatus = "Deactivated";
                _currentKey = "";
                PlayerPrefs.DeleteKey(LICENSE_KEY_PREF);
                PlayerPrefs.Save();

                StopHeartbeat();
                OnLicenseStateChanged?.Invoke(false, "Deactivated");
                callback?.Invoke(true, "License deactivated");

                Debug.Log("[KeyVault] License deactivated from this machine.");
            }));
        }

        /// <summary>
        /// Clear the stored license key without server-side deactivation.
        /// </summary>
        public void ClearStoredKey()
        {
            _currentKey = "";
            _isLicensed = false;
            _licenseStatus = "Not Validated";
            PlayerPrefs.DeleteKey(LICENSE_KEY_PREF);
            PlayerPrefs.Save();
            StopHeartbeat();
        }

        // ─── Heartbeat ───

        private void StartHeartbeat()
        {
            StopHeartbeat();

            if (config.heartbeatIntervalSeconds > 0)
            {
                _heartbeatCoroutine = StartCoroutine(HeartbeatLoop());
            }
        }

        private void StopHeartbeat()
        {
            if (_heartbeatCoroutine != null)
            {
                StopCoroutine(_heartbeatCoroutine);
                _heartbeatCoroutine = null;
            }
        }

        private IEnumerator HeartbeatLoop()
        {
            while (true)
            {
                yield return new WaitForSeconds(config.heartbeatIntervalSeconds);

                if (!string.IsNullOrEmpty(_currentKey))
                {
                    ValidateNow();
                }
            }
        }

        // ─── HTTP ───

        private IEnumerator SendValidationRequest(string endpoint, string key, Action<ValidationResponse> callback)
        {
            string url = config.serverUrl.TrimEnd('/') + endpoint;

            ValidationRequest request = new ValidationRequest
            {
                key = key,
                productCode = config.productCode,
                hardwareId = _hardwareId,
                machineName = _machineName
            };

            string jsonBody = JsonUtility.ToJson(request);
            byte[] bodyRaw = Encoding.UTF8.GetBytes(jsonBody);

            using (UnityWebRequest www = new UnityWebRequest(url, "POST"))
            {
                www.uploadHandler = new UploadHandlerRaw(bodyRaw);
                www.downloadHandler = new DownloadHandlerBuffer();
                www.SetRequestHeader("Content-Type", "application/json");
                www.timeout = 15;

                yield return www.SendWebRequest();

                if (www.result == UnityWebRequest.Result.Success)
                {
                    ValidationResponse response = JsonUtility.FromJson<ValidationResponse>(www.downloadHandler.text);
                    callback?.Invoke(response);
                }
                else
                {
                    Debug.LogError($"[KeyVault] Request failed: {www.error}");
                    callback?.Invoke(new ValidationResponse
                    {
                        valid = false,
                        reason = "NETWORK_ERROR"
                    });
                }
            }
        }

        private void OnApplicationQuit()
        {
            StopHeartbeat();
        }
    }
}
