# Holds W and captures a burst of frames, so view bob can be measured as
# inter-frame movement. Releases W afterwards.
param(
    [string]$Prefix = "burst",
    [int]$Frames = 8,
    [int]$DelayMs = 120,
    [string]$Scan = "0x11"
)

Add-Type -AssemblyName System.Drawing
Add-Type -TypeDefinition @'
using System; using System.Runtime.InteropServices; using System.Text;
public class WB {
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
  delegate bool EnumProc(IntPtr h, IntPtr l);
  public struct RECT { public int L,T,R,B; }
  const uint SCANCODE = 8, KEYUP = 2;

  public static IntPtr Find() {
    IntPtr found = IntPtr.Zero;
    EnumWindows((h,l) => {
      if (!IsWindowVisible(h)) return true;
      int n = GetWindowTextLength(h); if (n == 0) return true;
      var sb = new StringBuilder(n+1); GetWindowText(h, sb, sb.Capacity);
      uint pid; GetWindowThreadProcessId(h, out pid);
      try { var pr = System.Diagnostics.Process.GetProcessById((int)pid);
            if (pr.ProcessName.Contains("java") && sb.ToString().Contains("Tsunami")) { found = h; return false; } } catch {}
      return true;
    }, IntPtr.Zero);
    return found;
  }
  public static void KeyDown(ushort scan){ var a=new INPUT[1]; a[0].type=1; a[0].ki.wScan=scan; a[0].ki.dwFlags=SCANCODE; SendInput(1,a,Marshal.SizeOf(typeof(INPUT))); }
  public static void KeyUp(ushort scan){ var a=new INPUT[1]; a[0].type=1; a[0].ki.wScan=scan; a[0].ki.dwFlags=SCANCODE|KEYUP; SendInput(1,a,Marshal.SizeOf(typeof(INPUT))); }
  public static void Shot(IntPtr h,string p){ RECT r; GetWindowRect(h,out r);
    var b=new System.Drawing.Bitmap(r.R-r.L,r.B-r.T); var g=System.Drawing.Graphics.FromImage(b);
    g.CopyFromScreen(r.L,r.T,0,0,b.Size); b.Save(p,System.Drawing.Imaging.ImageFormat.Png); g.Dispose(); b.Dispose(); }
}
'@ -ReferencedAssemblies System.Drawing -ErrorAction SilentlyContinue

$h = [WB]::Find()
if ($h -eq [IntPtr]::Zero) { Write-Output "NO WINDOW"; exit 1 }
[void][WB]::SetForegroundWindow($h)
Start-Sleep -Milliseconds 900

[WB]::KeyDown([Convert]::ToUInt16($Scan,16))
Start-Sleep -Milliseconds 400   # let walking start so bob is mid-cycle
for ($i = 0; $i -lt $Frames; $i++) {
    [WB]::Shot($h, "$Prefix$i.png")
    Start-Sleep -Milliseconds $DelayMs
}
[WB]::KeyUp([Convert]::ToUInt16($Scan,16))
Write-Output "captured $Frames frames"
