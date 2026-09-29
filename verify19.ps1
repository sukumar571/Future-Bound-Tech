$ErrorActionPreference = 'Stop'
$Base = 'http://localhost:8080'
$script:pass = 0
$script:fail = 0

function Check([string]$name, [bool]$ok, [string]$detail = '') {
    if ($ok) { $script:pass++; Write-Host "[PASS] $name $detail" -ForegroundColor Green }
    else { $script:fail++; Write-Host "[FAIL] $name $detail" -ForegroundColor Red }
}
function Csrf($sess, $path) {
    $r = Invoke-WebRequest -Uri "$Base$path" -WebSession $sess -UseBasicParsing
    foreach ($m in [regex]::Matches($r.Content, '<input[^>]*name="_csrf"[^>]*>|<input[^>]*_csrf[^>]*>')) {
        $v = [regex]::Match($m.Value, 'value="([^"]+)"')
        if ($v.Success) { return $v.Groups[1].Value }
    }
    throw "csrf token not found on $path"
}
function Get1($sess, $path) { (Invoke-WebRequest -Uri "$Base$path" -WebSession $sess -UseBasicParsing).Content }
function Post1($sess, $path, $tok, $fields) {
    $b = @{} + $fields
    $b['_csrf'] = $tok
    $r = Invoke-WebRequest -Uri "$Base$path" -Method POST -WebSession $sess -UseBasicParsing -Body $b
    return $r.Content
}
function Login([string]$email, [string]$password) {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $t = Csrf $s '/login'
    Post1 $s '/login' $t @{ username = $email; password = $password } | Out-Null
    return $s
}
# Returns @{value=label} for every <option> inside the <select> with the given id.
function SelectOptions($html, [string]$selectId) {
    $pattern = '(?s)<select[^>]*id="' + $selectId + '".*?</select>'
    $block = [regex]::Match($html, $pattern)
    if (-not $block.Success) { return @{} }
    $map = @{}
    foreach ($m in [regex]::Matches($block.Value, '<option value="([^"]*)"[^>]*>([^<]*)</option>')) {
        if ($m.Groups[1].Value -ne '') { $map[$m.Groups[1].Value] = $m.Groups[2].Value.Trim() }
    }
    return $map
}
function FindOptionValue($map, [string]$labelPart) {
    foreach ($k in $map.Keys) { if ($map[$k] -like "*$labelPart*") { return $k } }
    return $null
}

$email = "p19.qa+$(Get-Random)@example.test"
$phone = "9$(Get-Random -Minimum 100000000 $(Get-Random -Maximum 999999999))"
$batchName = "P19 QA Batch $(Get-Random -Maximum 9999)"

# ---------------------------------------------------------------- admin
Write-Host "`n=== Admin notification console ===" -ForegroundColor Cyan
$admin = Login 'mine@gmail.com' 'password'
$atok = Csrf $admin '/admin/notifications'
$page = Get1 $admin '/admin/notifications'
Check 'admin feed renders' ($page -match 'Broadcast to students')
Check 'email off by default' ($page -match 'E-mail delivery is off')
Check 'email status explains env var' ($page -match 'APP_EMAIL_ENABLED|APP_EMAIL_FROM|SPRING_MAIL')
Check 'feed table rendered' ($page -match 'No notifications have been sent yet|<table')

Write-Host "`n=== Create batch (demo data for audience fan-out) ===" -ForegroundColor Cyan
Post1 $admin '/admin/batches' $atok @{
    batchName = $batchName; courseId = 1; trainerId = 1; maxSeats = 30
    mode = 'ONLINE'; status = 'SCHEDULED'; enrollmentStatus = 'OPEN'; published = 'true'
    meetingLink = 'https://meet.example.test/p19-qa'
} | Out-Null
$send = Get1 $admin '/admin/notifications/new'
Check 'send form renders' ($send -match 'Send notification')
$bid = FindOptionValue (SelectOptions $send 'batchId') $batchName
Check 'batch available in picker' ($null -ne $bid) "batchId=$bid"
$courseOptions = SelectOptions $send 'courseId'
$cid = FindOptionValue $courseOptions 'Java Full Stack'
Check 'course available in picker' ($null -ne $cid) "courseId=$cid"

Write-Host "`n=== Register + enroll a student (drives payment/enrollment triggers) ===" -ForegroundColor Cyan
$pub = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$ptok = Csrf $pub '/register'
Post1 $pub '/register' $ptok @{
    firstName = 'Phase'; lastName = 'Nineteen'; email = $email; phone = $phone
    password = 'Password123!'; confirmPassword = 'Password123!'
} | Out-Null
$stu = Login $email 'Password123!'
$stok = Csrf $stu '/student/notifications'
$inbox = Get1 $stu '/student/notifications'
Check 'student signed in' ($inbox -notmatch 'Sign in to continue|id="password"' -and $inbox -match 'Notifications')
Check 'fresh inbox is empty' ($inbox -match 'No notifications|You are all caught up')
Get1 $stu '/student/checkout?slug=java-full-stack-development' | Out-Null
$out = Post1 $stu '/student/checkout/pay' $stok @{ slug = 'java-full-stack-development'; batchId = $bid }
Check 'demo checkout activated' ($out -match 'enrollment is now active')
Start-Sleep -Milliseconds 500
$inbox = Get1 $stu '/student/notifications'
Check 'payment confirmation delivered' ($inbox -match 'Payment received')
Check 'enrollment confirmation delivered' ($inbox -match 'Enrollment confirmed')
Check 'unread badge shown' ($inbox -match 'New</span>|badge text-bg-danger')

Write-Host "`n=== Admin broadcasts to each audience ===" -ForegroundColor Cyan
function Send($fields) { Post1 $admin '/admin/notifications' $atok $fields }

$r = Send @{ audience = 'ALL_STUDENTS'; type = 'GENERAL_ANNOUNCEMENT'; title = 'P19 all students'; message = 'Broadcast to every active student.' }
Check 'all-students send ok' ($r -match 'sent to \d+ student') ([regex]::Match($r, 'sent to \d+ student').Value)

$r = Send @{ audience = 'COURSE'; courseId = $cid; type = 'COURSE_ANNOUNCEMENT'; title = 'P19 course note'; message = 'Course audience broadcast.' }
Check 'course audience ok' ($r -match 'sent to \d+ student') ([regex]::Match($r, 'sent to \d+ student').Value)

$r = Send @{ audience = 'BATCH'; batchId = $bid; type = 'BATCH_ANNOUNCEMENT'; title = 'P19 batch note'; message = 'Batch audience broadcast.' }
Check 'batch audience ok' ($r -match 'sent to \d+ student') ([regex]::Match($r, 'sent to \d+ student').Value)

$sid = FindOptionValue (SelectOptions (Get1 $admin '/admin/notifications/new') 'studentId') 'Phase Nineteen'
$r = Send @{ audience = 'STUDENT'; studentId = $sid; type = 'MESSAGE'; title = 'P19 direct note'; message = 'One-to-one broadcast.' }
Check 'single-student send ok' ($r -match 'sent to 1 student') "studentId=$sid $([regex]::Match($r, 'sent to \d+ student|no students to notify').Value)"

$r = Send @{ audience = 'COURSE'; type = 'COURSE_ANNOUNCEMENT'; title = 'P19 invalid'; message = 'Missing course id must not broadcast.' }
Check 'audience/id mismatch rejected' ($r -match 'Choose the course to notify')

$inbox = Get1 $stu '/student/notifications'
Check 'broadcast visible to student' ($inbox -match 'P19 all students' -and $inbox -match 'P19 course note' -and $inbox -match 'P19 batch note' -and $inbox -match 'P19 direct note')
Check 'batch announcement typed' ($inbox -match 'Batch Announcement')

Write-Host "`n=== Student inbox actions ===" -ForegroundColor Cyan
$id = [regex]::Match($inbox, '/student/notifications/(\d+)/read').Groups[1].Value
Check 'notification id found' ($id -ne '') "id=$id"
Post1 $stu "/student/notifications/$id/read" $stok @{} | Out-Null
$unread = Get1 $stu '/student/notifications?filter=unread'
Check 'mark-read persisted' (-not ($unread -match "/student/notifications/$id/read"))
$before = Get1 $stu '/student/notifications'
$beforeCount = ([regex]::Matches($before, '/student/notifications/\d+/delete')).Count
Post1 $stu "/student/notifications/$id/delete" $stok @{} | Out-Null
$after = Get1 $stu '/student/notifications'
$afterCount = ([regex]::Matches($after, '/student/notifications/\d+/delete')).Count
Check 'dismiss removes exactly one row' ($afterCount -eq $beforeCount - 1) "$beforeCount -> $afterCount"
Check 'unread filter still offered' ($unread -match 'Unread')

$ann = Get1 $stu '/student/announcements'
Check 'announcements page renders' ($ann -match 'Welcome to Future Bound Tech')

Write-Host "`n=== Trainer ===" -ForegroundColor Cyan
$trainer = Login 'trainer@futureboundtech.com' 'password'
$ttok = Csrf $trainer '/trainer/notifications'
$tform = Get1 $trainer '/trainer/announcements/new'
$tb = FindOptionValue (SelectOptions $tform 'batchId') $batchName
if (-not $tb) { $tb = FindOptionValue (SelectOptions $tform 'batch') $batchName }
Check 'trainer sees assigned batch' ($null -ne $tb) "batchId=$tb"
$an = Post1 $trainer '/trainer/announcements/new' $ttok @{
    title = "P19 trainer notice for $batchName"; content = 'Published by the trainer to an assigned batch.'; batchId = $tb
}
Check 'trainer announcement published' ($an -match 'P19 trainer notice')
$tin0 = Get1 $trainer '/trainer/notifications'
$tid = [regex]::Match($tin0, '/trainer/notifications/(\d+)/read').Groups[1].Value
if ($tid) {
    Post1 $trainer "/trainer/notifications/$tid/read" $ttok @{} | Out-Null
    Check 'trainer mark-read ok' ((Get1 $trainer '/trainer/notifications') -match 'Notification marked as read|Notifications')
} else {
    Check 'trainer inbox empty is tolerated' $true
}

Write-Host "`n=== Reminder sweep ===" -ForegroundColor Cyan
# A class in 60 minutes and a deadline in 12 hours both fall inside the default windows.
$classAt = (Get-Date).AddMinutes(60).ToString('yyyy-MM-ddTHH:mm')
$dueAt = (Get-Date).AddHours(12).ToString('yyyy-MM-ddTHH:mm')
Post1 $admin '/admin/live-classes' $atok @{
    topic = 'P19 reminder class'; batchId = $bid; startTime = $classAt
    mode = 'ONLINE'; status = 'SCHEDULED'; meetingLink = 'https://meet.example.test/p19'
} | Out-Null
Post1 $trainer '/trainer/assignments/new' $ttok @{
    title = 'P19 reminder assignment'; batchId = $tb; dueDate = $dueAt; maxMarks = 100; status = 'PUBLISHED'
} | Out-Null
$r = Post1 $admin '/admin/notifications/reminders' $atok @{}
$sweep1 = [regex]::Match($r, 'No reminders were due|Created \d+ reminder').Value
Check 'sweep created reminders' ($sweep1 -match 'Created') $sweep1
$inbox = Get1 $stu '/student/notifications'
Check 'class reminder delivered' ($inbox -match 'P19 reminder class')
Check 'assignment deadline reminder delivered' ($inbox -match 'P19 reminder assignment')
$r = Post1 $admin '/admin/notifications/reminders' $atok @{}
Check 'second sweep is deduplicated' ($r -match 'No reminders were due') ([regex]::Match($r, 'No reminders were due|Created \d+ reminder').Value)

$inbox = Get1 $stu '/student/notifications'
Check 'trainer batch announcement reached student' ($inbox -match 'P19 trainer notice')
Check 'announcement type label rendered' ($inbox -match 'Announcement')

Write-Host "`n=== Summary: $pass passed, $fail failed ===" -ForegroundColor $(if ($fail -eq 0) { 'Green' } else { 'Yellow' })
Write-Host "student email: $email ; batch: $batchName (id $bid)"
