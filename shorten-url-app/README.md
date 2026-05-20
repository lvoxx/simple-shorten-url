# 🔗 Link Shortener

A modern, full-stack URL shortener application built with Vue 3, TypeScript, and Firebase Functions. Create, manage, and track your shortened links with a beautiful, responsive interface.

[![Live Demo](https://img.shields.io/badge/demo-live-success)](https://johnrusu.github.io/vue-url-link-shortner/)
[![Vue 3](https://img.shields.io/badge/Vue-3.5-brightgreen.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-blue.svg)](https://www.typescriptlang.org/)
[![Firebase](https://img.shields.io/badge/Firebase-Functions-orange.svg)](https://firebase.google.com/)

## 🚀 Live Demo

**Production URL:** [https://johnrusu.github.io/vue-url-link-shortner/](https://johnrusu.github.io/vue-url-link-shortner/)

**API Endpoint:** `https://api-t23t3flzvq-uc.a.run.app`

## ✨ Features

- 🔐 **Secure Authentication** - Auth0 integration with JWT tokens and refresh token support
- 📊 **Analytics Dashboard** - Track clicks and engagement for each shortened link
- 🎨 **Modern UI** - Beautiful interface built with Vuetify and Tailwind CSS
- 🔄 **Real-time Updates** - Instant feedback on link creation and management
- 📱 **Responsive Design** - Works seamlessly on desktop, tablet, and mobile
- 🌐 **Custom Short Codes** - Create memorable custom URLs or use auto-generated codes
- 🗑️ **Full CRUD Operations** - Create, read, update, and delete links with ease
- 🔒 **User Isolation** - Each user's links are private and secure
- ⚡ **Fast Performance** - Optimized with Vite and Firebase Functions (2nd Gen)
- 🧪 **Tested** - Comprehensive unit and E2E tests with Vitest and Playwright

## 🛠️ Tech Stack

### Frontend

- **Vue 3.5** - Progressive JavaScript framework with Composition API
- **TypeScript 5.9** - Type-safe development
- **Vite** - Lightning-fast build tool
- **Vue Router 4** - Official routing solution
- **Pinia** - State management
- **Vuetify 3** - Material Design component framework
- **Tailwind CSS 4** - Utility-first CSS framework
- **Auth0 Vue SDK** - Authentication and authorization

### Backend

- **Firebase Functions (2nd Gen)** - Serverless backend on Cloud Run
- **Express.js** - Web application framework
- **MongoDB Atlas** - Cloud database with Mongoose ODM
- **Auth0** - JWT authentication with refresh tokens
- **Node.js 24** - Runtime environment

### DevOps & Tools

- **GitHub Actions** - CI/CD pipeline
- **GitHub Pages** - Static site hosting
- **Firebase Hosting** - API deployment
- **ESLint** - Code linting
- **Prettier** - Code formatting
- **Husky** - Git hooks
- **Vitest** - Unit testing
- **Playwright** - E2E testing

## 📋 Prerequisites

- **Node.js** 18+ and npm
- **Firebase CLI** (`npm install -g firebase-tools`)
- **Git**
- **MongoDB Atlas** account (free tier works)
- **Auth0** account (free tier works)
- **Firebase** project with Blaze (pay-as-you-go) plan

## 🔧 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/johnrusu/vue-link-shortner.git
cd vue-link-shortner
```

### 2. Install Dependencies

```bash
npm install
cd functions && npm install && cd ..
```

### 3. Environment Configuration

Create a `.env` file in the root directory:

```env
# Auth0 Configuration
VITE_AUTH0_DOMAIN=your-domain.auth0.com
VITE_AUTH0_CLIENT_ID=your-client-id
VITE_AUTH0_AUDIENCE=https://link-shortener-api

# API URLs
VITE_API_BASE_URL=https://your-firebase-function-url.run.app
VITE_API_BASE_URL_LOCAL=http://localhost:3000
```

Create a `functions/.env` file:

```env
# MongoDB Atlas
MONGO_URI=mongodb+srv://username:password@cluster.mongodb.net/linkshortener

# Auth0 API
AUTH0_AUDIENCE=https://link-shortener-api
AUTH0_ISSUER_BASE_URL=https://your-domain.auth0.com/
```

### 4. Auth0 Setup

1. Create an Auth0 application (Single Page Application)
2. Configure Allowed Callback URLs: `http://localhost:5173, https://johnrusu.github.io/vue-url-link-shortner/`
3. Configure Allowed Logout URLs: Same as callback URLs
4. Enable Grant Types: Authorization Code, Refresh Token
5. Create an Auth0 API with identifier: `https://link-shortener-api`
6. Enable "Allow Offline Access" in API settings

### 5. MongoDB Atlas Setup

1. Create a free cluster at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
2. Create a database user
3. Add IP whitelist: `0.0.0.0/0` (for Firebase Functions)
4. Get connection string and add to `functions/.env`

### 6. Firebase Setup

```bash
# Login to Firebase
firebase login

# Initialize Firebase (if not already done)
firebase init functions

# Select existing project or create new one
```

## 🚀 Development

### Local Development

```bash
# Start frontend dev server
npm run dev

# Start local backend server (separate terminal)
cd server
npm start
```

Visit `http://localhost:5173` to see the app.

### Build for Production

```bash
npm run build
```

### Deploy Backend to Firebase

```bash
firebase deploy --only functions
```

### Deploy Frontend to GitHub Pages

```bash
git add .
git commit -m "Deploy updates"
git push origin main
```

GitHub Actions will automatically build and deploy to GitHub Pages.

## 📝 Available Scripts

```bash
npm run dev          # Start development server
npm run build        # Build for production
npm run preview      # Preview production build
npm run lint         # Lint and fix files
npm run format       # Format code with Prettier
npm run test         # Run all tests
npm run test:e2e     # Run E2E tests only
npm run type-check   # Type check without building
```

## 🏗️ Project Structure

```
vue-link-shortner/
├── .github/
│   └── workflows/
│       └── static.yml          # GitHub Pages deployment
├── functions/
│   ├── server/
│   │   ├── constants/          # Environment variables
│   │   ├── database/           # MongoDB models and operations
│   │   ├── middleware/         # Auth middleware
│   │   └── routes/             # Express routes
│   ├── .env                    # Backend environment variables
│   └── index.js                # Firebase Functions entry point
├── public/
│   └── 404.html                # SPA fallback for GitHub Pages
├── server/                     # Local development server
├── src/
│   ├── assets/                 # Static assets
│   ├── components/             # Vue components
│   │   ├── AddLinkForm.vue
│   │   ├── HelloWorld.vue
│   │   └── LinksTable.vue
│   ├── constants/
│   │   └── routes.ts           # API endpoints configuration
│   ├── router/
│   │   └── index.ts            # Vue Router setup
│   ├── views/                  # Page components
│   │   ├── HomeView.vue
│   │   ├── LinkAnalytics.vue
│   │   ├── NotFound.vue
│   │   └── RedirectLinkAnalytics.vue
│   ├── App.vue                 # Root component
│   └── main.ts                 # App entry point
├── .env                        # Frontend environment variables
├── firebase.json               # Firebase configuration
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 🔑 Key Features Explained

### Authentication Flow

- Users authenticate via Auth0 with email/password or social login
- JWT access tokens with 1-hour expiration
- Refresh tokens for seamless re-authentication
- Tokens stored securely in localStorage
- Protected routes require valid authentication

### Link Management

- **Create**: Generate short links with custom or auto-generated codes
- **Read**: View all your links with analytics
- **Update**: Edit URL or short code
- **Delete**: Remove individual links or all at once
- **Analytics**: Track clicks and creation dates

### Security

- User-specific link isolation (userId filtering)
- CORS protection with credentials
- JWT token validation on all API requests
- MongoDB injection protection via Mongoose
- Auth0 token refresh on expiration

### Performance

- Client-side routing with no full page reloads
- Optimized bundle size with code splitting
- Firebase Functions 2nd Gen for faster cold starts
- MongoDB connection pooling
- Asset optimization with Vite

## 🌐 API Endpoints

| Method | Endpoint               | Description              | Auth |
| ------ | ---------------------- | ------------------------ | ---- |
| GET    | `/api/links`           | Get all user links       | ✅   |
| GET    | `/api/links/:id`       | Get link by ID           | ✅   |
| GET    | `/api/link/:shortCode` | Get link by short code   | ✅   |
| POST   | `/api/link`            | Create new link          | ✅   |
| PUT    | `/api/links/:id`       | Update link              | ✅   |
| DELETE | `/api/links/:id`       | Delete link              | ✅   |
| DELETE | `/api/links`           | Delete all links         | ✅   |
| GET    | `/:shortCode`          | Redirect to original URL | ❌   |

## 🧪 Testing

```bash
# Run all tests
npm test

# Run unit tests only
npx vitest run

# Run E2E tests only
npm run test:e2e

# Run tests in watch mode
npx vitest
```

## 🚢 Deployment

### Frontend (GitHub Pages)

1. Push to `main` branch
2. GitHub Actions automatically builds and deploys
3. Available at: `https://johnrusu.github.io/vue-url-link-shortner/`

### Backend (Firebase Functions)

```bash
firebase deploy --only functions
```

### Environment Variables for GitHub Actions

Add these secrets in GitHub repository settings:

- `VITE_AUTH0_CLIENT_ID`
- `VITE_AUTH0_DOMAIN`
- `VITE_AUTH0_AUDIENCE`
- `VITE_API_BASE_URL`

## 🐛 Troubleshooting

### "Missing Refresh Token" Error

- Enable Refresh Token grant type in Auth0 application
- Verify "Allow Offline Access" is enabled in Auth0 API
- Clear localStorage and re-authenticate

### 404 on Page Refresh

- The `public/404.html` file handles SPA routing on GitHub Pages
- Ensure it's included in the build output

### "User ID is required" Error

- Check Auth0 token contains `sub` claim
- Verify `checkJwt` middleware is applied to protected routes
- Check Firebase Functions logs for auth object structure

### MongoDB Connection Timeout

- Whitelist `0.0.0.0/0` in MongoDB Atlas Network Access
- Verify connection string in `functions/.env`
- Check MongoDB cluster is running

### CORS Errors

- Verify CORS configuration in `functions/index.js`
- Check `origin: true` and `credentials: true` are set
- Clear browser cache

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 👤 Author

**John Rusu**

- GitHub: [@johnrusu](https://github.com/johnrusu)

## 🙏 Acknowledgments

- [Vue.js](https://vuejs.org/) - The Progressive JavaScript Framework
- [Firebase](https://firebase.google.com/) - Backend and hosting platform
- [Auth0](https://auth0.com/) - Authentication service
- [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) - Cloud database
- [Vuetify](https://vuetifyjs.com/) - Material Design components
- [Vite](https://vitejs.dev/) - Next generation frontend tooling

---

⭐ Star this repo if you find it helpful!
