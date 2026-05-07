\newpage

# Security

The **Security** model in billetsys controls who can sign in, what they can access, and how role-based boundaries are enforced throughout the application.

![Password management page](images/22-security.png){ width=100% }

## Purpose

Billetsys is built around differentiated roles and scoped access. Security is what makes that possible. It ensures that users reach the right parts of the system and that ticket, company, and administrative data are not treated as universally available.

## Authentication

Authentication is the starting point of access control. A user signs in with account credentials, and the application uses that identity to establish an active session.

This allows billetsys to recognize the user across requests and present the correct role-specific experience.

## Authorization

After sign-in, authorization determines what the user is allowed to do. This is where the role model becomes active.

In practice, authorization defines access across areas such as:

* User-facing ticket views
* Company-scoped coordination views
* Support work areas
* Administrative configuration
* Reporting and oversight pages

This is one of the core design principles of billetsys because different roles are intentionally meant to see different scopes of work.

## Session behavior

Security in billetsys also depends on session handling. Once a user is authenticated, the session keeps the application connected to that identity until the user signs out or the session ends.

This makes the system usable in day-to-day work while still preserving the idea of authenticated access.

## Logout and account boundaries

Signing out is an important part of the same model. It closes the active session and ensures that the next interaction requires authentication again.

This is especially important in environments where multiple people may use the same device or shared workstation patterns exist.

## Password protection

Passwords are part of the security foundation of the system, but billetsys treats them as part of a broader access-control model rather than a standalone feature.

Together with session handling and role enforcement, password protection helps keep the application aligned with the responsibilities assigned to each user type.

## Why it matters

Without security controls, billetsys would lose the structure that makes its role model and customer scoping useful. With authentication, authorization, and session control in place, the platform can support collaboration while still preserving boundaries between users, roles, and organizations.
