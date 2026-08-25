# Reusable window driver for the Minecraft client.
# Usage: powershell -File mc.ps1 -Action shot -Out foo.png
#        powershell -File mc.ps1 -Action click -X 283 -Y 256
#        powershell -File mc.ps1 -Action key -Scan 0x36
#        powershell -File mc.ps1 -Action type -Text "FullBright"
param(
    [string]$Action = "shot",
    [string]$Out = "shot.png",
    [int]$X = 0,
    [int]$Y = 0,
    [string]$Scan = "0x36",
    [string]$Text = "",
    [int]$Wait = 0
)

Add-Type -AssemblyName System.Drawing
Add-Type -TypeDefinition @'
using System; using System.Collections.Generic; using System.Runtime.InteropServices; using System.Text;
public class MCW {
  [StructLayout(LayoutKind.Sequential)] public struct KEYBDINPUT { public ushort wVk,wScan; public uint dwFlags,time; public IntPtr dwExtraInfo; }
  [StructLayout(LayoutKind.Explicit, Size=40)] public struct INPUT { [FieldOffset(0)] public uint type; [FieldOffset(8)] public KEYBDINPUT ki; }
  [DllImport("user32.dll")] static extern uint SendInput(uint n, INPUT[] p, int cb);
  [DllImport("user32.dll")] static extern bool EnumWindows(EnumProc f, IntPtr l);
  [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] static extern int GetWindowTextLength(IntPtr h);
  [DllImport("user32.dll")] static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr h, out uint p);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr h,int x,int y,int w,int ht,bool r);
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x,int y);
  [DllImport("user32.dll")] public static extern void mouse_event(uint f,uint x,uint y,uint d,IntPtr e);
  delegate bool EnumProc(IntPtr h, IntPtr l);
  public struct RECT { public int L,T,R,B; }

  public static IntPtr Find() {
    IntPtr found = IntPtr.Zero;
    EnumWindows((h,l) => {
      if (!IsWindowVisible(h)) return true;
      int n = GetWindowTextLength(h); if (n == 0) return true;
      var sb = new StringBuilder(n+1); GetWindowText(h, sb, sb.Capacity);
      uint pid; GetWindowThreadProcessId(h, out pid);
      try {
        var pr = System.Diagnostics.Process.GetProcessById((int)pid);
        if (pr.ProcessName.Contains("java") && sb.ToString().Contains("Tsunami")) { found = h; return false; }
      } catch {}
      return true;
    }, IntPtr.Zero);
    return found;
  }
  public static string Title(IntPtr h) { int n=GetWindowTextLength(h); var sb=new StringBuilder(n+1); GetWindowText(h,sb,sb.Capacity); return sb.ToString(); }
  public static void Focus(IntPtr h){ MoveWindow(h,60,60,1280,800,true); SetForegroundWindow(h); System.Threading.Thread.Sleep(900); }
  public static void Tap(ushort s){ var a=new INPUT[1]; a[0].type=1; a[0].ki.wScan=s; a[0].ki.dwFlags=8; SendInput(1,a,Marshal.SizeOf(typeof(INPUT)));
    System.Threading.Thread.Sleep(80); var b=new INPUT[1]; b[0].type=1; b[0].ki.wScan=s; b[0].ki.dwFlags=8|2; SendInput(1,b,Marshal.SizeOf(typeof(INPUT))); }
  public static void Type(string t){ foreach(char c in t){ var a=new INPUT[1]; a[0].type=1; a[0].ki.wScan=c; a[0].ki.dwFlags=4; SendInput(1,a,Marshal.SizeOf(typeof(INPUT)));
    System.Threading.Thread.Sleep(35); var b=new INPUT[1]; b[0].type=1; b[0].ki.wScan=c; b[0].ki.dwFlags=4|2; SendInput(1,b,Marshal.SizeOf(typeof(INPUT))); System.Threading.Thread.Sleep(35);} }
  public static void Click(IntPtr h,int ox,int oy){ RECT r; GetWindowRect(h,out r); int x=r.L+ox,y=r.T+oy;
    for(int i=0;i<4;i++){ SetCursorPos(x-30+i*10,y); System.Threading.Thread.Sleep(80);} SetCursorPos(x,y); System.Threading.Thread.Sleep(350);
    mouse_event(0x0002,0,0,0,IntPtr.Zero); System.Threading.Thread.Sleep(100); mouse_event(0x0004,0,0,0,IntPtr.Zero); }
  public static void Shot(IntPtr h,string p){ RECT r; GetWindowRect(h,out r);
    var b=new System.Drawing.Bitmap(r.R-r.L,r.B-r.T); var g=System.Drawing.Graphics.FromImage(b);
    g.CopyFromScreen(r.L,r.T,0,0,b.Size); b.Save(p,System.Drawing.Imaging.ImageFormat.Png); g.Dispose(); b.Dispose(); }
}
'@ -ReferencedAssemblies System.Drawing -ErrorAction SilentlyContinue

$h = [MCW]::Find()
if ($h -eq [IntPtr]::Zero) { Write-Output "NO WINDOW"; exit 1 }
Write-Output ("window: " + [MCW]::Title($h))

switch ($Action) {
    "focus" { [MCW]::Focus($h) }
    "shot"  { [MCW]::Focus($h); if ($Wait -gt 0) { Start-Sleep -Seconds $Wait }; [MCW]::Shot($h, $Out); Write-Output "shot -> $Out" }
    "click" { [MCW]::Focus($h); [MCW]::Click($h, $X, $Y); if ($Wait -gt 0) { Start-Sleep -Seconds $Wait }; [MCW]::Shot($h, $Out); Write-Output "clicked $X,$Y -> $Out" }
    "key"   { [MCW]::Focus($h); [MCW]::Tap([Convert]::ToUInt16($Scan,16)); if ($Wait -gt 0) { Start-Sleep -Seconds $Wait }; [MCW]::Shot($h, $Out); Write-Output "key $Scan -> $Out" }
    "type"  { [MCW]::Focus($h); [MCW]::Type($Text); if ($Wait -gt 0) { Start-Sleep -Seconds $Wait }; [MCW]::Shot($h, $Out); Write-Output "typed -> $Out" }
}
