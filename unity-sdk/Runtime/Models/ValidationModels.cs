using System;

namespace KeyVault.SDK
{
    [Serializable]
    public class ValidationRequest
    {
        public string key;
        public string productCode;
        public string hardwareId;
        public string machineName;
    }

    [Serializable]
    public class ValidationResponse
    {
        public bool valid;
        public string reason;
        public string status;
        public string validUntil;
        public string productCode;
        public string customerName;
    }
}
