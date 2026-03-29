interface AppFooterProps {
  className?: string;
}

export default function AppFooter({ className }: AppFooterProps) {
  return (
    <footer className={className}>
      Copyright © {new Date().getFullYear()} Powered by{' '}
      <a href="https://github.com/mnemosyne-systems/billetsys" target="_blank" rel="noreferrer">
        billetsys
      </a>
    </footer>
  );
}
