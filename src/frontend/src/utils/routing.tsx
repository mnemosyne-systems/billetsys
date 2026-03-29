import type { MouseEventHandler, ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface RedirectJsonPayload {
  redirectTo?: string;
}

interface SmartLinkProps {
  href?: string | null;
  className?: string;
  children?: ReactNode;
  onClick?: MouseEventHandler<HTMLAnchorElement>;
}

export function normalizeClientPath(path?: string | null): string | null | undefined {
  if (typeof path !== 'string' || path.length === 0) {
    return path;
  }
  if (path.startsWith('/app/#')) {
    const cleanPath = path.slice('/app/#'.length);
    return cleanPath || '/';
  }
  if (path === '/app' || path === '/app/') {
    return '/';
  }
  if (path.startsWith('/app/')) {
    const cleanPath = path.slice('/app'.length);
    return cleanPath || '/';
  }
  return path;
}

export function isClientRoute(href?: string | null): boolean {
  if (typeof href !== 'string' || !href.startsWith('/')) {
    return false;
  }
  if (href.startsWith('/api/') || href === '/logout' || href.startsWith('/tickets/export/')) {
    return false;
  }
  if (href.startsWith('/attachments/')) {
    return !href.includes('/data');
  }
  return true;
}

export function resolveClientPath(path: string | null | undefined, fallback: string): string {
  if (!path) {
    return fallback;
  }
  return normalizeClientPath(path) || fallback;
}

export function resolveRedirectPath(response: Response | null | undefined, fallback: string): string {
  if (!response?.redirected || !response.url) {
    return fallback;
  }
  try {
    const redirectUrl = new URL(response.url, window.location.origin);
    if (redirectUrl.origin !== window.location.origin) {
      return fallback;
    }
    return resolveClientPath(`${redirectUrl.pathname}${redirectUrl.search}${redirectUrl.hash}`, fallback);
  } catch {
    return fallback;
  }
}

export async function resolvePostRedirectPath(response: Response, fallback: string): Promise<string> {
  const contentType = response?.headers?.get('content-type') || '';
  if (contentType.includes('application/json')) {
    const payload = (await response.json()) as RedirectJsonPayload;
    return resolveClientPath(payload?.redirectTo, fallback);
  }
  return resolveRedirectPath(response, fallback);
}

export function SmartLink({ href, className, children, onClick }: SmartLinkProps) {
  const normalizedHref = normalizeClientPath(href);
  if (isClientRoute(normalizedHref)) {
    return (
      <Link className={className} to={normalizedHref ?? '/'} onClick={onClick}>
        {children}
      </Link>
    );
  }

  return (
    <a className={className} href={normalizedHref || undefined} onClick={onClick}>
      {children}
    </a>
  );
}
