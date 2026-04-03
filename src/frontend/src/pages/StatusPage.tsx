/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { SmartLink } from "../utils/routing";
import type { StatusPageProps } from "../types/app";

export default function StatusPage({
  sessionState,
  title,
  message,
}: StatusPageProps) {
  const homeHref = sessionState.data?.homePath || "/login";

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
