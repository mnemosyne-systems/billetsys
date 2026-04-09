/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

interface AppFooterProps {
  className?: string;
}

export default function AppFooter({ className }: AppFooterProps) {
  return (
    <footer
      className={
        className ||
        "mt-auto bg-header-bg text-header-text text-center py-2.5 px-5 font-semibold"
      }
    >
      Copyright © {new Date().getFullYear()} Powered by{" "}
      <a
        href="https://github.com/mnemosyne-systems/billetsys"
        target="_blank"
        rel="noreferrer"
        className="text-header-text hover:underline"
      >
        billetsys
      </a>
    </footer>
  );
}
