/// <reference types="vite/client" />

declare module '*.css';

declare global {
  interface Window {
    Chart?: unknown;
  }
}

export {};
