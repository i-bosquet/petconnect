# PetConnect - Frontend

[![Spanish Version](https://img.shields.io/badge/Versión-Español-blue)](README_ES.md)

## Overview

PetConnect is a modern web application designed to bridge the gap between pet owners and veterinary clinics. The frontend provides an intuitive interface for users to manage their pets' healthcare, schedule appointments, access medical records, and communicate with veterinary professionals.

## Technologies & dependencies

### Core technologies
- **React** - UI library for building component-based interfaces
- **TypeScript** - Adds static typing to enhance code quality
- **Vite** - Fast frontend build tool with HMR (Hot Module Replacement)

### UI & styling
- **Tailwind CSS** - Utility-first CSS framework
- **Radix UI** - Accessible component primitives
- **Shadcn/ui** - Re-usable components built with Radix UI and Tailwind CSS

### State management & data fetching
- **React Query** - Data fetching and caching
- **Zustand** - Lightweight state management
- **Axios** - HTTP client

### Other tools
- **React Router** - Application routing
- **React Hook Form** - Form handling with validation
- **ESLint/Prettier** - Code quality and formatting

## Architecture & Directory structure

```
src/
├── assets/                 # Static assets (images, icons, fonts)
├── components/             # Reusable UI components organized by domain
│   ├── auth/               # Authentication related components (login, register)
│   ├── clinic/             # Clinic related components 
│   ├── common/             # Common components used across the application
│   ├── dashboard/          # Dashboard specific components
│   ├── layout/             # Layout components (header, footer, sidebar)
│   ├── pet/                # Pet management related components
│   ├── profile/            # User profile related components
│   └── ui/                 # Basic UI components (buttons, inputs, modals)
├── config/                 # Application configuration files
├── contexts/               # React context providers
├── hooks/                  # Custom React hooks for shared logic
│   ├── auth/               # Authentication related hooks
│   ├── clinic/             # Clinic related hooks
│   └── pet/                # Pet related hooks
├── layouts/                # Page layout templates and containers
│   ├── auth/               # Authentication page layouts
│   ├── dashboard/          # Dashboard layouts
│   └── public/             # Public page layouts
├── lib/                    # Utility libraries and third-party configs
├── pages/                  # Application page components
│   ├── auth/               # Authentication pages (login, register, forgot password)
│   ├── clinic/             # Clinic related pages
│   ├── dashboard/          # Dashboard pages
│   ├── error/              # Error pages (404, 500)
│   ├── home/               # Home page
│   ├── pet/                # Pet management pages
│   ├── profile/            # User profile pages
│   └── settings/           # User settings pages
├── routes/                 # Route definitions and navigation logic
│   ├── private/            # Private routes requiring authentication
│   └── public/             # Public routes
├── services/               # API services and data fetching
│   ├── auth/               # Authentication services
│   ├── clinic/             # Clinic related services
│   └── pet/                # Pet related services
├── store/                  # State management (Zustand stores)
│   ├── auth/               # Authentication state
│   ├── clinic/             # Clinic state
│   └── pet/                # Pet state
├── styles/                 # Global styles and Tailwind configuration
├── types/                  # TypeScript type definitions
│   ├── auth/               # Authentication related types
│   ├── clinic/             # Clinic related types
│   └── pet/                # Pet related types
└── utils/                  # Helper functions and utilities
    ├── date/               # Date formatting utilities
    ├── format/             # Text formatting utilities
    └── validation/         # Form validation utilities
```

### Separation of concerns

The directory structure follows a domain-driven approach that separates code by functionality. Components, services, and state management are organized to maintain clear boundaries between different parts of the application, making the codebase more maintainable and easier to navigate.

### Scalability

The modular organization allows the application to grow naturally as new features are added. Domain-specific components and services can be added without disrupting existing code, enabling multiple developers to work on different parts of the application simultaneously.

###  Reusability

Components are designed to be reusable, from basic UI elements to domain-specific components. This hierarchical approach ensures consistency throughout the application and reduces code duplication.

### Developer experience

The architecture provides a clear structure for different types of code, following established React patterns that are familiar to most developers. This makes it easier to find specific functionality and enables the gradual adoption of new patterns or technologies as needed.

## Getting Started

1. Clone the repository
```bash
git clone https://github.com/i-bosquet/petconnect.git
cd frontend
cd petconnect-frontend
```

2. Install dependencies
```bash
npm install
```

3. Start the development server
```bash
npm run dev
```

## Available scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint
- `npm run test` - Run tests

---
*Para obtener instrucciones en español, consulte [README_ES.md](README_ES.md).*
