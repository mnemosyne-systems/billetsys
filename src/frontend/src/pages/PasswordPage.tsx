/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "../auth/useAuth";

export default function PasswordPage() {
  const { keycloak } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (sessionStorage.getItem("password_update_success") === "true") {
      sessionStorage.removeItem("password_update_success");
      toast.success("Password updated successfully!");
      navigate("/profile", { replace: true });
      return;
    }

    keycloak.login({ action: "UPDATE_PASSWORD" });
  }, [keycloak, navigate]);

  return (
    <div className="flex flex-col items-center justify-center p-8 bg-background max-w-sm mx-auto text-center space-y-4 mt-12">
      <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary mx-auto"></div>
      <p className="text-muted-foreground font-medium text-sm">
        Redirecting to secure password update service...
      </p>
    </div>
  );
}
