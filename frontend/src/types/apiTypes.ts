/**
 * This file acts as the central barrel file for exporting all API and common types.
 * It re-exports types defined in more specific files within the types directory.
 * Existing imports referencing this file will continue to work after refactoring.
 */

// Re-export enums 
export * from './enumTypes';

// Re-export common types
export * from './commonTypes';

// Re-export auth related types
export * from './authTypes';

// Re-export clinic related types
export * from './clinicTypes';

// Re-export pet related types
export * from './petTypes';

// Re-export record related types
export * from './recordTypes';