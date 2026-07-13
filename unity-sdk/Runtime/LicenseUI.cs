using UnityEngine;

namespace KeyVault.SDK
{
    /// <summary>
    /// A simple built-in UI for license key activation.
    /// Attach to the same GameObject as KeyValidator, or any persistent object.
    /// Displays a centered activation prompt when the license is not valid.
    /// 
    /// You can replace this with your own custom UI — just call
    /// KeyValidator.Instance.Activate(key, callback) from your own scripts.
    /// </summary>
    public class LicenseUI : MonoBehaviour
    {
        private string _inputKey = "";
        private string _statusMessage = "";
        private bool _isProcessing = false;
        private bool _showUI = true;

        private GUIStyle _boxStyle;
        private GUIStyle _titleStyle;
        private GUIStyle _buttonStyle;
        private GUIStyle _inputStyle;
        private GUIStyle _statusStyle;
        private bool _stylesInitialized = false;

        private void Update()
        {
            if (KeyValidator.Instance == null) return;

            // Show UI only when not licensed
            _showUI = !KeyValidator.Instance.IsLicensed;
        }

        private void InitStyles()
        {
            if (_stylesInitialized) return;

            _boxStyle = new GUIStyle(GUI.skin.box);
            _boxStyle.padding = new RectOffset(30, 30, 30, 30);
            _boxStyle.normal.background = MakeTexture(2, 2, new Color(0.1f, 0.1f, 0.15f, 0.95f));

            _titleStyle = new GUIStyle(GUI.skin.label);
            _titleStyle.fontSize = 22;
            _titleStyle.fontStyle = FontStyle.Bold;
            _titleStyle.alignment = TextAnchor.MiddleCenter;
            _titleStyle.normal.textColor = Color.white;

            _buttonStyle = new GUIStyle(GUI.skin.button);
            _buttonStyle.fontSize = 14;
            _buttonStyle.fixedHeight = 40;
            _buttonStyle.normal.textColor = Color.white;
            _buttonStyle.normal.background = MakeTexture(2, 2, new Color(0.39f, 0.4f, 0.95f, 1f));

            _inputStyle = new GUIStyle(GUI.skin.textField);
            _inputStyle.fontSize = 16;
            _inputStyle.fixedHeight = 36;
            _inputStyle.alignment = TextAnchor.MiddleCenter;

            _statusStyle = new GUIStyle(GUI.skin.label);
            _statusStyle.fontSize = 12;
            _statusStyle.alignment = TextAnchor.MiddleCenter;
            _statusStyle.normal.textColor = new Color(1f, 0.5f, 0.5f, 1f);

            _stylesInitialized = true;
        }

        private void OnGUI()
        {
            if (!_showUI || KeyValidator.Instance == null) return;

            InitStyles();

            float width = 440;
            float height = 260;
            Rect windowRect = new Rect(
                (Screen.width - width) / 2,
                (Screen.height - height) / 2,
                width, height
            );

            // Dim background
            GUI.DrawTexture(new Rect(0, 0, Screen.width, Screen.height),
                MakeTexture(1, 1, new Color(0, 0, 0, 0.7f)));

            GUILayout.BeginArea(windowRect, _boxStyle);

            GUILayout.Label("🔑 License Activation", _titleStyle);
            GUILayout.Space(20);

            GUILayout.Label("Enter your license key:", GUI.skin.label);
            GUILayout.Space(5);

            _inputKey = GUILayout.TextField(_inputKey, _inputStyle);
            GUILayout.Space(15);

            GUI.enabled = !_isProcessing && !string.IsNullOrEmpty(_inputKey);

            if (GUILayout.Button(_isProcessing ? "Validating..." : "Activate License", _buttonStyle))
            {
                _isProcessing = true;
                _statusMessage = "Validating...";

                KeyValidator.Instance.Activate(_inputKey.Trim(), (success, message) =>
                {
                    _isProcessing = false;
                    _statusMessage = success ? "✅ License activated!" : $"❌ {message}";
                });
            }

            GUI.enabled = true;

            if (!string.IsNullOrEmpty(_statusMessage))
            {
                GUILayout.Space(10);
                GUILayout.Label(_statusMessage, _statusStyle);
            }

            GUILayout.EndArea();
        }

        private Texture2D MakeTexture(int width, int height, Color color)
        {
            Color[] pixels = new Color[width * height];
            for (int i = 0; i < pixels.Length; i++)
                pixels[i] = color;

            Texture2D texture = new Texture2D(width, height);
            texture.SetPixels(pixels);
            texture.Apply();
            return texture;
        }
    }
}
