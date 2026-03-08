import { useState } from "react";
import { useAuthStore } from "../store/useAuthStore";
import BorderAnimatedContainer from "../components/BorderAnimatedContainer";
import { 
  TicketIcon, 
  MailIcon, 
  LoaderIcon, 
  LockIcon,
  UsersIcon,
  ShieldCheckIcon,
  HeadphonesIcon,
  UserCogIcon,
  BuildingIcon,
  MessageSquareIcon,
  InboxIcon,
  CheckCircleIcon,
  ClockIcon
} from "lucide-react";
import { Link } from "react-router";

function LoginPage() {
  const [formData, setFormData] = useState({ email: "", password: "" });
  const { login, isLoggingIn } = useAuthStore();

  const handleSubmit = (e) => {
    e.preventDefault();
    login(formData);
  };

  const roles = [
    { icon: UsersIcon, name: "User", color: "text-blue-400", bg: "bg-blue-500/10" },
    { icon: UserCogIcon, name: "Superuser", color: "text-purple-400", bg: "bg-purple-500/10" },
    { icon: HeadphonesIcon, name: "TAM", color: "text-green-400", bg: "bg-green-500/10" },
    { icon: ShieldCheckIcon, name: "Support", color: "text-yellow-400", bg: "bg-yellow-500/10" },
    { icon: BuildingIcon, name: "Admin", color: "text-red-400", bg: "bg-red-500/10" }
  ];

  const features = [
    { icon: TicketIcon, text: "Ticket System", color: "text-cyan-400" },
    { icon: ShieldCheckIcon, text: "TLS v1.3", color: "text-emerald-400" },
    { icon: MailIcon, text: "Email Integration", color: "text-blue-400" },
    { icon: MessageSquareIcon, text: "Markdown Editor", color: "text-purple-400" }
  ];

  const stats = [
    { icon: InboxIcon, value: "5", label: "Roles", color: "text-cyan-400" },
    { icon: ClockIcon, value: "24/7", label: "Support", color: "text-emerald-400" },
    { icon: CheckCircleIcon, value: "100%", label: "Secure", color: "text-blue-400" }
  ];

  return (
    <div className="min-h-screen w-full flex items-center justify-center p-4 bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
      <div className="relative w-full max-w-6xl md:h-[800px] h-[650px]">
        <BorderAnimatedContainer>
          <div className="w-full flex flex-col md:flex-row h-full">
           
            <div className="md:w-1/2 p-8 flex items-center justify-center md:border-r border-cyan-500/30">
              <div className="w-full max-w-md">
               
                <div className="text-center mb-8">
                  <div className="flex justify-center mb-4">
                    <div className="relative">
                      <div className="absolute inset-0 bg-cyan-400 rounded-full blur-2xl opacity-30 animate-pulse"></div>
                      <TicketIcon className="w-16 h-16 text-cyan-400 relative" />
                    </div>
                  </div>
                  <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent mb-2">
                    billetsys
                  </h1>
                  <h2 className="text-2xl font-bold text-slate-200 mb-2">Welcome Back</h2>
                  <p className="text-slate-400">Login to your support dashboard</p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      Email Address
                    </label>
                    <div className="relative group">
                      <MailIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="email"
                        value={formData.email}
                        onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                        className="w-full pl-10 pr-4 py-3 bg-slate-800/50 border border-slate-700 rounded-lg focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/50 text-slate-200 placeholder-slate-500 transition-all"
                        placeholder="matter@billetsys.com"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      Password
                    </label>
                    <div className="relative group">
                      <LockIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-cyan-400 transition-colors" />
                      <input
                        type="password"
                        value={formData.password}
                        onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                        className="w-full pl-10 pr-4 py-3 bg-slate-800/50 border border-slate-700 rounded-lg focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/50 text-slate-200 placeholder-slate-500 transition-all"
                        placeholder="Enter your password"
                        required
                      />
                    </div>
                  </div>

                  <div className="flex justify-end">
                    <Link to="/forgot-password" className="text-sm text-cyan-400 hover:text-cyan-300 transition-colors">
                      Forgot password?
                    </Link>
                  </div>

                  <button 
                    className="w-full py-3 px-4 bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600 text-white font-medium rounded-lg transition-all duration-200 transform hover:scale-[1.02] focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:ring-offset-2 focus:ring-offset-slate-900 disabled:opacity-50 disabled:cursor-not-allowed"
                    type="submit" 
                    disabled={isLoggingIn}
                  >
                    {isLoggingIn ? (
                      <div className="flex items-center justify-center">
                        <LoaderIcon className="w-5 h-5 animate-spin mr-2" />
                        <span>Signing in...</span>
                      </div>
                    ) : (
                      "Sign In"
                    )}
                  </button>
                </form>

                <div className="mt-6 text-center">
                  <p className="text-slate-400">
                    Don't have an account?{' '}
                    <Link to="/signup" className="text-cyan-400 hover:text-cyan-300 font-medium transition-colors">
                      Sign Up
                    </Link>
                  </p>
                </div>

                <div className="mt-8 text-center">
                  <div className="flex items-center justify-center gap-2 text-xs text-slate-500">
                    <ShieldCheckIcon className="w-4 h-4" />
                    <span>End-to-end encrypted</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="hidden md:w-1/2 md:flex flex-col items-center justify-center p-8 bg-gradient-to-br from-slate-800/50 to-transparent">
              
              <div className="grid grid-cols-3 gap-4 mb-8 w-full max-w-md">
                {stats.map((stat, index) => (
                  <div key={index} className="text-center p-3 bg-slate-800/30 rounded-lg border border-slate-700/50">
                    <stat.icon className={`w-6 h-6 ${stat.color} mx-auto mb-1`} />
                    <div className="text-lg font-bold text-slate-200">{stat.value}</div>
                    <div className="text-xs text-slate-400">{stat.label}</div>
                  </div>
                ))}
              </div>

              <h3 className="text-2xl font-bold text-slate-200 mb-4 text-center">
                Modern Support Ticket Solution
              </h3>
              
              <p className="text-slate-400 mb-8 max-w-md mx-auto text-center">
                Streamlined ticketing system designed for all roles to navigate and get work done quicker
              </p>

              <div className="grid grid-cols-2 gap-3 max-w-md mx-auto mb-8 w-full">
                {roles.map((role, index) => (
                  <div 
                    key={index} 
                    className={`flex items-center gap-2 p-2 ${role.bg} rounded-lg border border-slate-700/50 hover:border-${role.color.replace('text-', '')}/30 transition-colors group`}
                  >
                    <div className={`p-1.5 rounded-md ${role.bg} group-hover:scale-110 transition-transform`}>
                      <role.icon className={`w-4 h-4 ${role.color}`} />
                    </div>
                    <span className="text-sm text-slate-300">{role.name}</span>
                  </div>
                ))}
              </div>

              <div className="grid grid-cols-2 gap-3 max-w-md mx-auto">
                {features.map((feature, index) => (
                  <div key={index} className="flex items-center gap-2 p-2">
                    <feature.icon className={`w-4 h-4 ${feature.color}`} />
                    <span className="text-xs text-slate-400">{feature.text}</span>
                  </div>
                ))}
              </div>

              <div className="mt-8 flex justify-center gap-2">
                {[...Array(5)].map((_, i) => (
                  <div
                    key={i}
                    className="w-2 h-8 bg-gradient-to-t from-cyan-500 to-blue-500 rounded-full animate-pulse"
                    style={{ 
                      animationDelay: `${i * 0.1}s`,
                      height: `${20 + i * 8}px`
                    }}
                  />
                ))}
              </div>
            </div>
          </div>
        </BorderAnimatedContainer>
      </div>
    </div>
  );
}

export default LoginPage;