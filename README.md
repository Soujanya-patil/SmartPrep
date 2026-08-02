# SmartPrep
AI-powered learning platform for NEET and placement prep

.\mvnw spring-boot:run   
it usually means: navigate to the project's root directory and execute the Maven Wrapper script there.

# 🎯 SmartPrep — AI-Powered Study & Placement Platform

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)
![Next.js](https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=next.js&logoColor=white)
![Google Gemini](https://img.shields.io/badge/Google_Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white)

> SmartPrep is an AI-powered learning platform that helps students prepare for NEET exams and placement interviews. Built with Java Spring Boot, MySQL, Google Gemini AI, and Next.js.

---

## ✨ Features

### 📚 NEET Study Module
- **AI Quiz Generator** — Generates fresh MCQ questions on any topic using Google Gemini AI
- **Weak Topic Detector** — Analyzes quiz performance and flags topics below 60% accuracy
- **Study Streak Tracker** — Duolingo-style streak counter to keep students motivated
- **Smart Chatbot** — Personalized AI assistant that knows your weak topics and gives targeted advice
- **Video Recommendations** — AI recommends real YouTube NEET videos based on weak topics
- **Attention Check System** — Generates fresh AI questions every 15 minutes during video sessions

### 💼 Placement Module
- **AI Mock Interview** — 10-question sessions with Technical, Aptitude, and HR questions
- **Real-time Answer Evaluation** — AI evaluates each answer and gives instant feedback
- **Interview Report** — Final score, strengths, and improvement areas

### 📊 Dashboard
- **Sibling Dashboard** — Single API showing streak, quiz stats, weak topics, and AI motivation message
- **Study Log** — Daily study hour tracking with subject coverage

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3, Spring AI |
| Database | MySQL 8, Spring Data JPA, Hibernate |
| AI | Google Gemini API (via Spring AI) |
| Frontend | Next.js 14, TypeScript, Tailwind CSS |
| Video | YouTube Data API v3 |
| Testing | Postman |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- MySQL 8+
- Node.js 18+
- Google Gemini API Key
- YouTube Data API v3 Key

### Backend Setup

1. Clone the repo:
```bash
git clone https://github.com/YOUR_USERNAME/SmartPrep.git
cd SmartPrep
```

2. Create MySQL database:
```sql
CREATE DATABASE smartprep;
```

3. Copy the example properties file:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

4. Fill in your API keys in `application.properties`

5. Run the backend:
```bash
.\mvnw spring-boot:run
```

Backend runs on `http://localhost:8081`

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3000`

---

## 📡 API Endpoints

### User
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/register` | Register a new user |
| POST | `/api/users/login` | Login |

### Quiz
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/quiz/generate` | Generate AI quiz questions |
| POST | `/api/results/submit` | Submit quiz result |
| GET | `/api/results/weak-topics/{userId}` | Get weak topics |
| GET | `/api/results/history/{userId}` | Get quiz history |

### Study
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/study/log` | Log daily study |
| GET | `/api/study/streak/{userId}` | Get study streak |

### Chatbot
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chat` | Chat with AI assistant |

### Videos
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/videos/recommend/{userId}` | Get AI video recommendations |
| GET | `/api/videos/search` | Search videos by topic |
| GET | `/api/videos/attention-check` | Get attention check question |

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard/{userId}` | Get full student dashboard |

### Mock Interview
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/interview/start` | Start mock interview |
| POST | `/api/interview/evaluate` | Evaluate answer |
| POST | `/api/interview/report` | Get final report |

---

## 🗄️ Database Schema

```sql
users           — User accounts
study_log       — Daily study tracking
quiz_questions  — Cached AI questions
quiz_results    — Student quiz performance
```

---

## 👩‍💻 Author

**Soujanya Patil**
- B.Tech in Artificial Intelligence & Data Science
- Angadi Institute of Technology and Management
- Skills: Java, Spring Boot, MySQL, REST APIs, Spring AI

---

## 🙏 Acknowledgements

- Google Gemini AI for powering the intelligent features
- YouTube Data API for video recommendations
- Vercel for frontend hosting
- Railway for backend hosting