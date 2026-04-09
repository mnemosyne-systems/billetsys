/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { SmartLink } from "../utils/routing";
import type { StatusPageProps } from "../types/app";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "../components/ui/card";
import { Button } from "../components/ui/button";

export default function StatusPage({
  sessionState,
  title,
  message,
}: StatusPageProps) {
  const homeHref = sessionState.data?.homePath || "/login";

  return (
    <Card className="w-full max-w-2xl mx-auto mt-12 bg-card">
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl font-bold">{title}</CardTitle>
        <p className="text-muted-foreground">{message}</p>
      </CardHeader>
      <CardContent className="flex flex-wrap gap-3">
        <Button asChild>
          <SmartLink href={homeHref}>Return to app</SmartLink>
        </Button>
      </CardContent>
    </Card>
  );
}
