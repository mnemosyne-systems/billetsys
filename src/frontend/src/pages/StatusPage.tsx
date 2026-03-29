import { SmartLink } from '../utils/routing';
import type { StatusPageProps } from '../types/app';

export default function StatusPage({ sessionState, title, message }: StatusPageProps) {
  const homeHref = sessionState.data?.homePath || '/login';

  return (
    <section className="panel auth-panel">
      <div className="section-header">
        <div>
          <h2>{title}</h2>
          <p className="section-copy">{message}</p>
        </div>
      </div>
      <div className="button-row">
        <SmartLink className="primary-button" href={homeHref}>
          Return to app
        </SmartLink>
      </div>
    </section>
  );
}
