# Holds a key (or clicks) while polling the client's session readouts, and
# reports min/mean/max of one field.
#
# Focus and sampling have to happen in the SAME process. Doing the focus click
# in one call and the key hold in another loses focus in between, the game
# never receives the key, and the readout sits at zero - which looks exactly
# like a broken feature rather than a broken test.
#
#   powershell -File scripts/harness/hold-and-sample.ps1 -Url <session-url> -Field speed -Scan 0x11 -Seconds 4
#   powershell -File scripts/harness/hold-and-sample.ps1 -Url <session-url> -Field cps.left -Click left -Seconds 4
#
# -Scan holds a keyboard key for the duration. -Click spams a mouse button
# instead, which is how CPS gets a non-zero reading.

param(
    [Parameter(Mandatory = $true)][string]$Url,
    [string]$Field = "speed",
    [string]$Scan = "",
    [ValidateSet("", "left", "right")][string]$Click = "",
    [double]$Seconds = 4,
    [int]$ClicksPerSecond = 8,
    [string]$ShotPrefix = ""
)

Add-Type -AssemblyName System.Drawing
Add-Type -TypeDefinition @'
using System; using System.Runtime.InteropServices; using System.Text;
public class HS {
  [StructLayout(LayoutKind.Sequential)] public struct KEYBDINPUT { public ushort wVk,wScan; public uint dwFlags,time; public IntPtr dwExtraInfo; }
  [StructLayout(LayoutKind.Sequential)] public struct MOUSEINPUT { public int dx,dy; public uint mouseData,dwFlags,time; public IntPtr dwExtraInfo; }
  [StructLayout(LayoutKind.Explicit, Size=40)] public struct INPUT { [FieldOffset(0)] public uint type; [FieldOffset(8)] public KEYBDINPUT ki; [FieldOffset(8)] public MOUSEINPUT mi; }
  [DllImport("user32.dll")] static extern uint SendInput(uint n, INPUT[] p, int cb);
  [DllImport("user32.dll")] static extern bool EnumWindows(EnumProc f, IntPtr l);
  [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] static extern int GetWindowTextLength(IntPtr h);
  [DllImport("user32.dll")] static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint p);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
  [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr h,int x,int y,int w,int ht,bool r);
  [DllImport("user32.dll")] static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")] static extern bool AttachThreadInput(uint attach, uint attachTo, bool fAttach);
  [DllImport("kernel32.dll")] static extern uint GetCurrentThreadId();

  /**
   * Windows refuses SetForegroundWindow from a process that does not already
   * own the foreground, and it fails silently. Keyboard input then goes to the
   * console instead of the game while MOUSE input still arrives, because an
   * in-game Minecraft has grabbed the cursor and receives it regardless of
   * focus. That asymmetry is why clicks registered and W did not, and why the
   * readout looked broken when it was reporting a genuinely motionless player.
   *
   * Attaching to the foreground window's input queue lifts the restriction.
   */
  public static void Focus(IntPtr h){
    MoveWindow(h,60,60,1280,800,true);
    uint ignored;
    uint target = GetWindowThreadProcessId(h, out ignored);
    uint self = GetCurrentThreadId();
    uint fore = GetWindowThreadProcessId(GetForegroundWindow(), out ignored);
    AttachThreadInput(self, fore, true);
    AttachThreadInput(target, fore, true);
    SetForegroundWindow(h);
    AttachThreadInput(target, fore, false);
    AttachThreadInput(self, fore, false);
    System.Threading.Thread.Sleep(900);
  }
  public static void Shot(IntPtr h,string p){ RECT r; GetWindowRect(h,out r);
    var b=new System.Drawing.Bitmap(r.R-r.L,r.B-r.T); var g=System.Drawing.Graphics.FromImage(b);
    g.CopyFromScreen(r.L,r.T,0,0,b.Size); b.Save(p,System.Drawing.Imaging.ImageFormat.Png); g.Dispose(); b.Dispose(); }
  delegate bool EnumProc(IntPtr h, IntPtr l);
  public struct RECT { public int L,T,R,B; }
  const uint SCANCODE = 8, KEYUP = 2;
  const uint MOUSE_LEFTDOWN = 0x0002, MOUSE_LEFTUP = 0x0004, MOUSE_RIGHTDOWN = 0x0008, MOUSE_RIGHTUP = 0x0010;

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
  public static void ClickLeft(){ var a=new INPUT[1]; a[0].type=0; a[0].mi.dwFlags=MOUSE_LEFTDOWN; SendInput(1,a,Marshal.SizeOf(typeof(INPUT)));
    System.Threading.Thread.Sleep(15); var b=new INPUT[1]; b[0].type=0; b[0].mi.dwFlags=MOUSE_LEFTUP; SendInput(1,b,Marshal.SizeOf(typeof(INPUT))); }
  public static void ClickRight(){ var a=new INPUT[1]; a[0].type=0; a[0].mi.dwFlags=MOUSE_RIGHTDOWN; SendInput(1,a,Marshal.SizeOf(typeof(INPUT)));
    System.Threading.Thread.Sleep(15); var b=new INPUT[1]; b[0].type=0; b[0].mi.dwFlags=MOUSE_RIGHTUP; SendInput(1,b,Marshal.SizeOf(typeof(INPUT))); }
}
'@ -ReferencedAssemblies System.Drawing -ErrorAction SilentlyContinue

$h = [HS]::Find()
if ($h -eq [IntPtr]::Zero) { Write-Output "NO WINDOW"; exit 1 }

[HS]::Focus($h)

$scanCode = 0
if ($Scan -ne "") { $scanCode = [Convert]::ToUInt16($Scan, 16); [HS]::KeyDown($scanCode) }

# Captured so that "the readout stayed at zero" can be told apart from "nothing
# actually happened", which look identical in the numbers alone.
if ($ShotPrefix -ne "") { [HS]::Shot($h, ($ShotPrefix + "start.png")) }

$values = @()
$deadline = (Get-Date).AddSeconds($Seconds)
$clickGap = [int](1000 / [Math]::Max(1, $ClicksPerSecond))
$lastClick = Get-Date

while ((Get-Date) -lt $deadline) {
    if ($Click -ne "" -and ((Get-Date) - $lastClick).TotalMilliseconds -ge $clickGap) {
        if ($Click -eq "left") { [HS]::ClickLeft() } else { [HS]::ClickRight() }
        $lastClick = Get-Date
    }

    try {
        $data = Invoke-RestMethod -Uri $Url -TimeoutSec 3
        $v = $data
        foreach ($part in $Field.Split(".")) { $v = $v.$part }
        if ($v -ne $null) { $values += [double]$v }
    } catch { }

    Start-Sleep -Milliseconds 100
}

if ($ShotPrefix -ne "") { [HS]::Shot($h, ($ShotPrefix + "end.png")) }
if ($scanCode -ne 0) { [HS]::KeyUp($scanCode) }

if ($values.Count -eq 0) { Write-Output "no samples"; exit 1 }

$stats = $values | Measure-Object -Minimum -Maximum -Average
Write-Output ("{0}: samples={1} min={2:N2} mean={3:N2} max={4:N2}" -f $Field, $values.Count, $stats.Minimum, $stats.Average, $stats.Maximum)
