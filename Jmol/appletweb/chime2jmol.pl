use strict;
use HTML::Parser;
use Getopt::Std;
use File::Find;
use File::Copy;
use File::Path;

sub handleEmbed;
sub handleEnd;
sub usage;

use vars qw/$opt_v $opt_a $opt_s $opt_c $opt_d $opt_b/;
getopts('va:s:cd:b:') or usage();

($opt_s && $opt_d) or usage();

print "source directory $opt_s\n" if $opt_v;
(-d $opt_s) or die "$opt_s is not a directory";

if ($opt_c) {
    print "deleting directory tree $opt_d\n" if $opt_v;
    rmtree $opt_d;
}

if (-e $opt_d) {
    print "destination directory $opt_d exists\n" if $opt_v;
    -d $opt_d or die "$opt_d is not a directory";
} else {
    print "creating destination directory $opt_d\n" if $opt_v;
    mkdir($opt_d) or die "could not make directory $opt_d";
}

my $archive = "JmolApplet.jar";
$archive = $opt_a if $opt_a;
print "archive is $archive\n" if $opt_v;

my $codebase = $opt_b;
print "codebase is $codebase\n" if $codebase && $opt_v;

my $baseDirectory;
my @files;
my @directories;
sub accumulateFilename {
    if ($baseDirectory) {
	my $pathname = $File::Find::name;
	my $name = substr $pathname, length($baseDirectory);
	push @files, $name if -f $pathname;
	push @directories, $name if -d $pathname;
    } else {
	$baseDirectory = $File::Find::name;
    }
}
find(\&accumulateFilename, $opt_s);

for my $directory (@directories) {
    print "mkdir $opt_d$directory\n" if $opt_v;
    mkdir "$opt_d$directory";
}

for my $file (@files) {
    next if $file =~  /\~$/; # ignore emacs files
    print "processing $file\n" if $opt_v;
    processFile("$baseDirectory$file", "$opt_d$file");
}
exit();

sub processFile {
    my ($src, $dst) = @_;
    if ($src =~ /html?$/i) {
	processHtmlFile($src, $dst);
    } else {
	copyFile($src, $dst);
    }
}

sub copyFile {
    my ($src, $dst) = @_;
#    print "copyFile $src -> $dst\n";
    copy $src, $dst;
}

sub processHtmlFile {
    my ($src, $dst) = @_;
    open OUTPUT, ">$dst" or die "could not open $dst";

    my $p = HTML::Parser->new(start_h =>
			      [\&handleEmbed, 'skipped_text,text,tokens'],
			      end_document_h => [\&writePrevious,
						 'skipped_text']);
    $p->report_tags('embed');
    $p->parse_file($src) || die $!;
    close OUTPUT;
}

my ($previous, $embed, $tokens);
my $tokenCount;

# common to both plugins and buttons
my ($name, $width, $height, $bgcolor, $src);
# plug-in specific
my ($loadStructCallback, $messageCallback, $pauseCallback, $pickCallback);
# button-specific
my ($type, $button, $buttonCallback, $target, $script, $altscript);

sub handleEmbed {
    ($previous, $embed, $tokens) = @_;
    $tokenCount = scalar @$tokens;

    $name = getParameter('name');
    $width = getParameter('width');
    $height = getParameter('height');
    $bgcolor = getParameter('bgcolor');
    $src = getParameter('src');

    $loadStructCallback = getParameter('LoadStructCallback');
    $messageCallback = getParameter('MessageCallback');
    $pauseCallback = getParameter('pauseCallback');
    $pickCallback = getParameter('pickCallback');

    $type = getParameter('type');
    $button = getParameter('button');
    $buttonCallback = getParameter('ButtonCallBack');
    $target = getParameter('target');
    $script = getParameter('script');
    $script = convertSemicolonNewline($script);
    $altscript = convertSemicolonNewline(getParameter('altscript'));
    writePrevious($previous);
    writeCommentedEmbed();
#    dumpVars();
    writeJmolApplet() unless $button;
    writeButtonControl() if $button;
}

sub dumpVars() {
    print <<END;
    name=$name
    width=$width
    height=$height
    bgcolor=$bgcolor
    src=$src
    type=$type
    button=$button
    buttonCallback=$buttonCallback
    target=$target
    script=$script
    altscript=$altscript
    
END
}

sub writePrevious {
    print OUTPUT convertNewline(@_[0]);
}

sub writeCommentedEmbed {
    $embed = convertNewline($embed);
    print OUTPUT "<!-- $embed -->\n";
}

sub writeJmolApplet {
    print OUTPUT
	"  <applet name=$name code='JmolApplet' archive='$archive'\n"
	if $name;
    print OUTPUT
	"  <applet code='JmolApplet' archive='$archive'\n"
	unless $name;
    print OUTPUT
	"          codebase='$codebase'\n" if $codebase;
    print OUTPUT
	"          width='$width' height='$height' mayscript='true' >\n";
    print OUTPUT
	"    <param name='emulate' value='chime' />\n";
    print OUTPUT
	"    <param name='bgcolor' value='$bgcolor' />\n" if $bgcolor;
    print OUTPUT
	"    <param name='load'    value='$src' />\n" if $src;
    print OUTPUT
	"    <param name='script'  value='$script' />\n" if $script;
    print OUTPUT
	"    <param name='LoadStructCallback' value='$loadStructCallback' />\n"
	if $loadStructCallback;
    print OUTPUT
	"    <param name='MessageCallback'    value='$messageCallback' />\n"
	if $messageCallback;
    print OUTPUT
	"    <param name='PauseCallback'      value='$pauseCallback' />\n"
	if $pauseCallback;
    print OUTPUT
	"    <param name=PickCallback       value=$pickCallback />\n"
	if $pickCallback;
    print OUTPUT
	"  </applet>\n";
}

sub writeButtonControl {
    my ($controlType, $group);
    if ($button =~ /push/i) {
	$controlType = "chimePush";
    } elsif ($button =~ /toggle/i) {
	$controlType = "chimeToggle";
    } elsif ($button =~ /radio(\d+)/i) {
	$controlType = "chimeRadio";
	$group = $1;
    }
    my $buttonScript = $script || $src;
    print OUTPUT
	"  <applet name=$name code='JmolAppletControl' archive='$archive'\n"
	if $name;
    print OUTPUT
	"  <applet code='JmolAppletControl' archive='$archive'\n"
	unless $name;
    print OUTPUT
	"          codebase='$codebase'\n" if $codebase;
    print OUTPUT
	"          width='$width' height='$height' mayscript='true' >\n";
    print OUTPUT
	"    <param name='target' value=$target />\n".
	"    <param name='type'   value='$controlType' />\n";
    print OUTPUT
	"    <param name='group'  value='$group' />\n"
	if $group;
    print OUTPUT
	"    <param name='script' value=$buttonScript />\n"
	if $buttonScript;
    print OUTPUT
	"    <param name='altscript' value=$altscript />\n"
	if $altscript;
    print OUTPUT
	"    <param name='ButtonCallback' value=$buttonCallback />\n"
	if $buttonCallback;
    print OUTPUT
	"  </applet>\n";
}

sub getParameter {
    my ($tag) = @_;
    for (my $i = 0; $i < $tokenCount; ++$i) {
	my $token = $tokens->[$i];
	return $tokens->[$i + 1] if ($token =~ /$tag/i);
    }
    return undef;
}

sub convertNewline {
    my ($text) = @_;
    $text =~ s/\r\n/\n/g;
    $text =~ s/\r/\n/g;
    return $text;
}

sub convertSemicolonNewline {
    my ($text) = @_;
    $text = convertNewline($text);
    $text =~ s/\n/;\n/g;
    return $text;
}

sub usage {
    print <<END;
    perl chime2jmol.pl -s <src> -d <dst> {-c} {-a <archive>}

    -s <source directory>
    -d <destination directory>
    -c Clear destination directory
    -a <archive> specify alternate archive name
    -b <path> specify codebase
    -v Verbose
END
    exit;
}
