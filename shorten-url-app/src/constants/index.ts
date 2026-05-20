import { routes } from "./routes";

const APP = {
  TITLE: "Link Shortener",
  DESCRIPTION: "Create, manage, and track shortened links with ease",
};

const LABELS = {
  LOGIN: "Log In",
  LOGOUT: "Log Out",
  HOME: "Home",
  USER: "User",
  ACCOUNT: "Account",
  LOADING: "Loading...",
  ERROR_TITLE: "Oops!",
  ERROR_MESSAGE: "Something went wrong",
  WELCOME_TITLE: `Welcome to ${APP.TITLE}`,
  BUTTON_LOGIN_TITLE: "Log in to your account",
  NO_ANIMATION: "No Animation Data Found",
  INVALID_JSON: "Invalid JSON Data",
  MISSING_DATA: "No Animation Data Provided",
  ACTIONS: "Actions",
  DELETE: "Delete",
  CONFIRM_DELETE_TITLE: "Confirm Deletion",
  CONFIRM_DELETE_MESSAGE: "Are you sure you want to delete this link?",
  CONFIRM: "Confirm",
  CANCEL: "Cancel",
  VIEW_DETAILS: "View Details",
  LINKS_TABLE_TITLE: "Links",
  DELETE_ALL: (links: number) =>
    `Are you sure you want to delete all ${links} links? This action cannot be undone.`,
  ADD_LINK_TITLE: "Add New Link",
  URL_LABEL: "Original URL",
  SHORT_CODE_LABEL: "Short Code",
  ADD_LINK: "Add Link",
  DELETE_ALL_LINKS: "Delete All Links",
  NOT_FOUND_TITLE: "404",
  NOT_FOUND_HEADING: "Page Not Found",
  NOT_FOUND_MESSAGE:
    "The page you're looking for doesn't exist or has been moved.",
  GO_HOME: "Go Home",
  GO_BACK: "Go Back",
  BACK_TO_DASHBOARD: "Back to Dashboard",
  LINK_DETAILS: "Link Details",
  ORIGINAL_URL: "Original URL",
  SHORT_URL: "Short URL",
  CREATED_AT: "Created At",
  TOTAL_CLICKS: "Total Clicks",
  CLICK_STATISTICS: "Click Statistics",
  QUICK_STATS: "Quick Stats",
  CLICK_THROUGH_RATE: "Click Through Rate",
  CLICKS_RECORDED: (clicks: number) => `${clicks} clicks recorded`,
  DAYS_ACTIVE: "Days Active",
  DAYS_COUNT: (days: number) => `${days} days`,
  LINK_STATUS: "Link Status",
  ACTIVE: "Active",
  CONFIRM_DELETE_LINK: "Are you sure you want to delete this link?",
  LINK_DELETED: "Link deleted successfully",
  FAILED_DELETE_LINK: "Failed to delete link",
  ERROR_DELETING_LINK: "An error occurred while deleting the link",
  SHORT_URL_COPIED: "Short URL copied to clipboard!",
  FAILED_COPY_URL: "Failed to copy URL",
  FAILED_LOAD_LINK: "Failed to load link details",
  ERROR_LOADING_LINK: "An error occurred while loading link details",
  URL_REQUIRED: "URL is required",
  INVALID_URL: "Please enter a valid URL",
  URL_PLACEHOLDER: "https://example.com",
  SHORT_CODE_PLACEHOLDER: "my-link (optional)",
  SHORT_CODE_HINT: "Leave empty to auto-generate",
  SHORT_CODE_PATTERN_ERROR:
    "Only letters, numbers, hyphens, and underscores allowed",
  SHORT_CODE_LENGTH_ERROR: "Short code must be 50 characters or less",
  LINK_CREATED: "Link created successfully",
  FAILED_CREATE_LINK: "Failed to create link",
  ERROR_CREATING_LINK: "An error occurred while creating the link",
  ABOUT: "About",
  LINK_ANALYTICS: "Analytics",
  FIX_AUTH_ISSUE: "Fix Authentication Issue",
  REMOVE_STORAGE: "Remove Stored Data.",
  RELOGIN: "Please log in again to continue.",
  IMPORT_LINKS: "Import Links",
  IMPORT_SUCCESS: (count: number) => `Successfully imported ${count} links.`,
  IMPORT_FAILED: "Failed to import links. Please check logs for details.",
  INVALID_FILE_TYPE: "Invalid file type. Please upload a JSON file.",
  IMPORT_LINKS_INSTRUCTIONS:
    "To import links, upload a JSON file containing an array of link objects with 'url' and optional 'shortCode' properties.",
};

const ADD_LINK_TABS = {
  ADD_LINK: "add-link",
  IMPORT_LINKS: "import-links",
};

const LINKS_MAPPING = {
  _id: "ID",
  url: "Original URL",
  shortCode: "Short Code",
  createdAt: "Created At",
  clicks: "Click Count",
};

const PLACEHOLDER_IMAGE = `data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%2363b3ed'/%3E%3Cpath d='M50 45c7.5 0 13.64-6.14 13.64-13.64S57.5 17.72 50 17.72s-13.64 6.14-13.64 13.64S42.5 45 50 45zm0 6.82c-9.09 0-27.28 4.56-27.28 13.64v3.41c0 1.88 1.53 3.41 3.41 3.41h47.74c1.88 0 3.41-1.53 3.41-3.41v-3.41c0-9.08-18.19-13.64-27.28-13.64z' fill='%23fff'/%3E%3C/svg%3E`;

const ABOUT_PAGE = {
  TITLE: `About ${APP.TITLE}`,
  WHAT_IS: {
    TITLE: `What is ${APP.TITLE}?`,
    DESCRIPTION_1: `${APP.TITLE} is a modern, full-stack URL shortener application built with Vue 3, TypeScript, and Firebase Functions. It provides a secure and efficient way to create, manage, and track shortened links with comprehensive analytics.`,
    DESCRIPTION_2:
      "Our goal is to make link management simple and powerful, whether you're sharing links on social media, tracking marketing campaigns, or organizing your personal bookmarks.",
  },
  FEATURES: {
    TITLE: "Features",
    SECURE_AUTH: {
      TITLE: "Secure Authentication",
      DESCRIPTION:
        "Auth0 integration with JWT tokens and refresh token support",
    },
    ANALYTICS: {
      TITLE: "Analytics Dashboard",
      DESCRIPTION: "Track clicks and engagement for each shortened link",
    },
    MODERN_UI: {
      TITLE: "Modern UI",
      DESCRIPTION: "Beautiful interface built with Vuetify and Tailwind CSS",
    },
    RESPONSIVE: {
      TITLE: "Responsive Design",
      DESCRIPTION: "Works seamlessly on desktop, tablet, and mobile",
    },
    CUSTOM_CODES: {
      TITLE: "Custom Short Codes",
      DESCRIPTION: "Create memorable custom URLs or use auto-generated codes",
    },
    USER_ISOLATION: {
      TITLE: "User Isolation",
      DESCRIPTION: "Each user's links are private and secure",
    },
  },
  TECHNOLOGIES: {
    TITLE: "Technologies Used",
    STACK: [
      "Vue 3.5",
      "TypeScript 5.9",
      "Vite",
      "Vuetify 3",
      "Tailwind CSS 4",
      "Firebase Functions",
      "MongoDB Atlas",
      "Auth0",
      "Express.js",
      "Pinia",
    ],
  },
  HOW_TO_USE: {
    TITLE: "How to Use",
    STEPS: [
      "Sign in with your Auth0 account",
      "Enter the long URL you want to shorten",
      "Optionally customize your short code or let the system generate one",
      "View and manage all your links from the dashboard",
      "Track analytics and clicks for each link",
    ],
  },
  DEVELOPER: {
    TITLE: "About the Developer",
    NAME: "Ionut Rusu",
    ROLE: "Fullstack Developer",
    BIO_1:
      "Ionut Rusu is a passionate Fullstack Developer based in Romania, specializing in Vue.js, Node.js, React.js, and UI/UX design. With expertise in modern web technologies and a keen eye for user experience, Ionut creates efficient and elegant solutions for complex problems.",
    BIO_2:
      "This Link Shortener application showcases the power of modern JavaScript frameworks and demonstrates a commitment to building practical, user-friendly tools with enterprise-grade security and scalability.",
    PORTFOLIO: "Visit Portfolio",
    GITHUB: "GitHub",
    LINKEDIN: "LinkedIn",
    PORTFOLIO_URL: "https://rusu-ionut.ro",
    GITHUB_URL: "https://github.com/johnrusu",
    LINKEDIN_URL: "https://www.linkedin.com/in/ionut-rusu-1035b112",
  },
  CONTACT: {
    TITLE: "Contact & Support",
    DESCRIPTION: "Have questions or feedback? We'd love to hear from you!",
    BUTTON: "Contact Us",
    URL: "https://www.rusu-ionut.ro/contact",
  },
};

const EXTENSIONS = {
  JSON: {
    DESCRIPTION: "JSON Files",
    EXTENSION: ".json",
    MIME_TYPE: "application/json",
  },
};

export {
  APP,
  LABELS,
  PLACEHOLDER_IMAGE,
  routes,
  LINKS_MAPPING,
  ABOUT_PAGE,
  ADD_LINK_TABS,
  EXTENSIONS,
};
