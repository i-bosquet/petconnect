# PetConnect - Frontend

[![English Version](https://img.shields.io/badge/Version-English-blue)](README.md)

## Descripción general

PetConnect es una aplicación web moderna diseñada para conectar a dueños de mascotas con clínicas veterinarias. El frontend proporciona una interfaz intuitiva para que los usuarios gestionen la salud de sus mascotas, programen citas, accedan a registros médicos y se comuniquen con profesionales veterinarios.

## Tecnologías y dDependencias

### Tecnologías principales
- **React** - Biblioteca de UI para construir interfaces basadas en componentes
- **TypeScript** - Añade tipado estático para mejorar la calidad del código
- **Vite** - Herramienta de construcción frontend rápida con HMR (Hot Module Replacement)

### UI y estilos
- **Tailwind CSS** - Framework CSS basado en utilidades
- **Radix UI** - Componentes primitivos accesibles
- **Shadcn/ui** - Componentes reutilizables construidos con Radix UI y Tailwind CSS

### Gestión de estado y obtención de datos
- **React Query** - Obtención y caché de datos
- **Zustand** - Gestión de estado ligera
- **Axios** - Cliente HTTP

### Otras herramientas
- **React Router** - Enrutamiento de la aplicación
- **React Hook Form** - Manejo de formularios con validación
- **ESLint/Prettier** - Calidad de código y formateo

## Arquitectura y estructura de directorios

```
src/
├── assets/                 # Activos estáticos (imágenes, iconos, fuentes)
├── components/             # Componentes UI reutilizables organizados por dominio
│   ├── auth/               # Componentes relacionados con la autenticación (login, registro)
│   ├── clinic/             # Componentes relacionados con clínicas
│   ├── common/             # Componentes comunes utilizados en toda la aplicación
│   ├── dashboard/          # Componentes específicos del dashboard
│   ├── layout/             # Componentes de diseño (cabecera, pie de página, barra lateral)
│   ├── pet/                # Componentes relacionados con la gestión de mascotas
│   ├── profile/            # Componentes relacionados con el perfil de usuario
│   └── ui/                 # Componentes UI básicos (botones, inputs, modales)
├── config/                 # Archivos de configuración de la aplicación
├── contexts/               # Proveedores de contexto de React
├── hooks/                  # Hooks personalizados de React para lógica compartida
│   ├── auth/               # Hooks relacionados con la autenticación
│   ├── clinic/             # Hooks relacionados con clínicas
│   └── pet/                # Hooks relacionados con mascotas
├── layouts/                # Plantillas de diseño de páginas y contenedores
│   ├── auth/               # Diseños para páginas de autenticación
│   ├── dashboard/          # Diseños para el dashboard
│   └── public/             # Diseños para páginas públicas
├── lib/                    # Bibliotecas de utilidades y configuraciones de terceros
├── pages/                  # Componentes de páginas de la aplicación
│   ├── auth/               # Páginas de autenticación (login, registro, contraseña olvidada)
│   ├── clinic/             # Páginas relacionadas con clínicas
│   ├── dashboard/          # Páginas del dashboard
│   ├── error/              # Páginas de error (404, 500)
│   ├── home/               # Página de inicio
│   ├── pet/                # Páginas de gestión de mascotas
│   ├── profile/            # Páginas de perfil de usuario
│   └── settings/           # Páginas de configuración de usuario
├── routes/                 # Definiciones de rutas y lógica de navegación
│   ├── private/            # Rutas privadas que requieren autenticación
│   └── public/             # Rutas públicas
├── services/               # Servicios API y obtención de datos
│   ├── auth/               # Servicios de autenticación
│   ├── clinic/             # Servicios relacionados con clínicas
│   └── pet/                # Servicios relacionados con mascotas
├── store/                  # Gestión de estado (stores de Zustand)
│   ├── auth/               # Estado de autenticación
│   ├── clinic/             # Estado de clínicas
│   └── pet/                # Estado de mascotas
├── styles/                 # Estilos globales y configuración de Tailwind
├── types/                  # Definiciones de tipos de TypeScript
│   ├── auth/               # Tipos relacionados con la autenticación
│   ├── clinic/             # Tipos relacionados con clínicas
│   └── pet/                # Tipos relacionados con mascotas
└── utils/                  # Funciones auxiliares y utilidades
    ├── date/               # Utilidades de formateo de fechas
    ├── format/             # Utilidades de formateo de texto
    └── validation/         # Utilidades de validación de formularios
```


### Separación de responsabilidades

La estructura de directorios sigue un enfoque dirigido por dominios que separa el código por funcionalidad. Los componentes, servicios y gestión de estado están organizados para mantener límites claros entre las diferentes partes de la aplicación, haciendo el código más mantenible y fácil de navegar.

### Escalabilidad

La organización modular permite que la aplicación crezca naturalmente a medida que se añaden nuevas características. Los componentes y servicios específicos de dominio pueden añadirse sin interrumpir el código existente, permitiendo que múltiples desarrolladores trabajen en diferentes partes de la aplicación simultáneamente.

### Reusabilidad

Los componentes están diseñados para ser reutilizables, desde elementos UI básicos hasta componentes específicos de dominio. Este enfoque jerárquico asegura la consistencia en toda la aplicación y reduce la duplicación de código.

### Experiencia del desarrollador

La arquitectura proporciona una estructura clara para diferentes tipos de código, siguiendo patrones establecidos de React que son familiares para la mayoría de los desarrolladores. Esto facilita encontrar funcionalidades específicas y permite la adopción gradual de nuevos patrones o tecnologías según sea necesario.

## Primeros Pasos

1. Clonar el repositorio
```bash
git clone https://github.com/i-bosquet/petconnect.git
cd frontend
```

2. Instalar dependencias
```bash
npm install
```

3. Iniciar el servidor de desarrollo
```bash
npm run dev
```

## Scripts disponibles

- `npm run dev` - Iniciar servidor de desarrollo
- `npm run build` - Construir para producción
- `npm run preview` - Previsualizar versión de producción
- `npm run lint` - Ejecutar ESLint
- `npm run test` - Ejecutar pruebas

*For English instructions, please see [README.md](README.md).*