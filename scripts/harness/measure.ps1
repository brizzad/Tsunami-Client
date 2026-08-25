# Image measurement for the FullBright / NoBob before-after tests.
#   -Mode luma  -A img.png                 -> mean luminance over the region
#   -Mode diff  -A img1.png -B img2.png    -> mean absolute pixel difference
# Region defaults to the world view, excluding HUD edges and the hotbar.
param(
    [string]$Mode = "luma",
    [string]$A,
    [string]$B,
    [int]$X = 260,
    [int]$Y = 120,
    [int]$W = 760,
    [int]$H = 500
)

Add-Type -AssemblyName System.Drawing

function Get-Pixels([string]$path, [int]$x, [int]$y, [int]$w, [int]$h) {
    $bmp = [System.Drawing.Bitmap]::FromFile($path)
    $x = [Math]::Max(0, [Math]::Min($x, $bmp.Width - 1))
    $y = [Math]::Max(0, [Math]::Min($y, $bmp.Height - 1))
    $w = [Math]::Min($w, $bmp.Width - $x)
    $h = [Math]::Min($h, $bmp.Height - $y)
    $rect = New-Object System.Drawing.Rectangle($x, $y, $w, $h)
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly,
                          [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $bytes = New-Object byte[] ($data.Stride * $h)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
    $bmp.UnlockBits($data)
    $bmp.Dispose()
    return @{ Bytes = $bytes; Stride = $data.Stride; W = $w; H = $h }
}

if ($Mode -eq "luma") {
    $p = Get-Pixels $A $X $Y $W $H
    [double]$sum = 0; [int]$n = 0
    for ($row = 0; $row -lt $p.H; $row++) {
        $base = $row * $p.Stride
        for ($col = 0; $col -lt $p.W; $col++) {
            $i = $base + $col * 4
            # BGRA order
            $sum += 0.0722 * $p.Bytes[$i] + 0.7152 * $p.Bytes[$i + 1] + 0.2126 * $p.Bytes[$i + 2]
            $n++
        }
    }
    "{0:N2}" -f ($sum / $n)
}
elseif ($Mode -eq "diff") {
    $pa = Get-Pixels $A $X $Y $W $H
    $pb = Get-Pixels $B $X $Y $W $H
    [double]$sum = 0; [int]$n = 0
    $h = [Math]::Min($pa.H, $pb.H); $w = [Math]::Min($pa.W, $pb.W)
    for ($row = 0; $row -lt $h; $row++) {
        $ba = $row * $pa.Stride; $bb = $row * $pb.Stride
        for ($col = 0; $col -lt $w; $col++) {
            $ia = $ba + $col * 4; $ib = $bb + $col * 4
            $sum += [Math]::Abs([int]$pa.Bytes[$ia]     - [int]$pb.Bytes[$ib])
            $sum += [Math]::Abs([int]$pa.Bytes[$ia + 1] - [int]$pb.Bytes[$ib + 1])
            $sum += [Math]::Abs([int]$pa.Bytes[$ia + 2] - [int]$pb.Bytes[$ib + 2])
            $n += 3
        }
    }
    "{0:N3}" -f ($sum / $n)
}
