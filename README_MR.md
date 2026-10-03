# Jieshuo AI Voice Assistant — Personal Prototype

ही पहिली prototype आवृत्ती आहे.

## काय करते
1. Android SpeechRecognizer वापरून बोललेले वाक्य transcription करते.
2. Gemini API ला text पाठवून भाषा जपून professional rewrite करते.
3. परिणाम ऐकवते आणि clipboard मध्ये कॉपी करते.
4. स्वतंत्र Accessibility Service सक्षम केल्यास focused editable field मध्ये Android Paste action करण्याचा प्रयत्न करते.

## महत्त्वाचे
- Gemini API key इथे/चॅटमध्ये देऊ नका.
- API key या personal prototype मध्ये device वर local SharedPreferences मध्ये ठेवली जाते. Production app साठी backend/proxy अधिक सुरक्षित आहे.
- Gemini model/endpoint उपलब्धतेनुसार बदलू शकतो.
- Android 15 वर पहिल्यांदा Microphone permission आणि Accessibility Service permission द्यावी लागेल.
- Jieshuo Lua integration पुढच्या टप्प्यात जोडायचे आहे.
