function BorderAnimatedContainer({ children }) {
  return (
    <div className="relative w-full h-full">
    
      <div
        className="absolute inset-0 rounded-2xl p-[2px] animate-border"
        style={{
          background: `conic-gradient(from var(--border-angle), 
               rgba(71, 85, 105, 0.48) 0deg, 
               rgba(71, 85, 105, 0.48) 288deg, 
               rgb(6, 182, 212) 309deg, 
               rgb(103, 232, 249) 324deg, 
               rgb(6, 182, 212) 338deg, 
               rgba(71, 85, 105, 0.48) 360deg)`,
        }}
      >
        
        <div className="w-full h-full rounded-2xl bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex overflow-hidden">
          {children}
        </div>
      </div>
    </div>
  );
}
export default BorderAnimatedContainer;