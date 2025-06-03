# Resume Tweaker - AI-Powered Resume Builder  

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://)  
[![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)  
[![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white)](https://openai.com/)  
[![Offline-First](https://img.shields.io/badge/Offline-First-green?style=for-the-badge)](https://)

Resume Tweaker is a lightweight Android app that generates professional **Resumes** and **Cover Letters** using OpenAI's API. No backend servers, no Firebase - just a clean APK that respects your privacy!

## ✨ Features  

- **AI-Optimized Resumes**: Tailored for specific job descriptions (ATS-friendly)  
- **Instant Cover Letters**: Professionally generated in seconds  
- **100% Offline Processing**: All AI calls go directly to OpenAI from your device  
- **PDF Export**: Save documents locally using iText7  
- **No Ads**: Completely ad-free experience  

## 📸 Screenshots  
![WhatsApp Image 2025-06-03 at 3 54 16 PM](https://github.com/user-attachments/assets/fb558c72-38d8-4acf-a038-2ba3fc9053a8)
![WhatsApp Image 2025-06-03 at 3 54 16 PM(1)](https://github.com/user-attachments/assets/37d3d00c-a6f5-444c-94a8-66e4890f4716)
![WhatsApp Image 2025-06-03 at 3 54 16 PM(2)](https://github.com/user-attachments/assets/ffe8f692-f854-4225-813a-cf16f6b7f9c0)
![WhatsApp Image 2025-06-03 at 3 54 17 PM](https://github.com/user-attachments/assets/9c9a4615-c00a-4544-8c23-5b49b66c2bf2)
![WhatsApp Image 2025-06-03 at 3 54 17 PM(1)](https://github.com/user-attachments/assets/3746ee08-4690-4f9b-a429-4f160f28c193)
![WhatsApp Image 2025-06-03 at 3 54 17 PM(2)](https://github.com/user-attachments/assets/2ddf9cc7-3bdc-4302-9b61-f178b0702714)


## ⚙️ Setup & Usage  

1. **Get OpenAI API Key**:
   - Create account at [OpenAI](https://platform.openai.com/)
   - Add payment method (minimum $5 credit required)
   - Generate API key from [API Keys page](https://platform.openai.com/account/api-keys)

2. **Install & Configure**:
   - Download APK (or build from source)
   - Paste your OpenAI API key in settings

3. **Generate Documents**:
   - Fill your resume details
   - Paste target job description
   - Click generate and download PDF

## 📦 Tech Stack  

```gradle
// Core Android
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.appcompat)
implementation(libs.material)

// Architecture Components
implementation(libs.androidx.lifecycle.viewmodel.ktx)
implementation(libs.androidx.navigation.fragment.ktx)

// PDF Generation
implementation("com.itextpdf:itext7-core:7.2.5")

// Networking
implementation("com.squareup.okhttp3:okhttp:4.9.3")
```

❓ FAQ

Q: Why does it need an OpenAI API key?
A: All AI processing happens through OpenAI's API (your device → OpenAI). No middle servers!

Q: Is my data stored anywhere?
A: Never! All processing happens on your device and temporary API calls to OpenAI.

Q: Can I use it without paying?
A: You need minimal OpenAI credit ($5) as they charge per request (typically <$0.10 per resume).

🤝 Contributing

Pull requests welcome! For major changes, please open an issue first.

💡 Pro Tip: For best results, provide detailed job descriptions when generating documents.
