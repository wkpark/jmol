use HTML::Parser;
use strict;

sub handleEmbed;
sub handleEnd;

my $p = HTML::Parser->new(start_h =>
  [\&handleEmbed, 'skipped_text,text,tokens'],
			  end_document_h => [\&writePrevious, 'skipped_text']);
$p->report_tags('embed');
$p->parse_file(shift || die) || die $!;

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
    $tokenCount = length $tokens;

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
    $script = convertSemicolonNewline(getParameter('script'));
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
    print convertNewline(@_[0]);
}

sub writeCommentedEmbed {
    $embed = convertNewline($embed);
    print "<!-- $embed -->\n";
}

sub writeJmolApplet {
    print
	"  <applet name=$name code=JmolApplet archive=JmolApplet.jar\n"
	if $name;
    print
	"  <applet code=JmolApplet archive=JmolApplet.jar\n"
	unless $name;
    print
	"          width=$width height=$height mayscript >\n";
    print
	"    <param name=bgcolor value=$bgcolor >\n" if $bgcolor;
    print
	"    <param name=load    value=$src >\n" if $src;
    print
	"    <param name=script  value=$script >\n" if $script;
    print
	"    <param name=LoadStructCallback value=$loadStructCallback >\n"
	if $loadStructCallback;
    print
	"    <param name=MessageCallback    value=$messageCallback >\n"
	if $messageCallback;
    print
	"    <param name=PauseCallback      value=$pauseCallback >\n"
	if $pauseCallback;
    print
	"    <param name=PickCallback       value=$pickCallback >\n"
	if $pickCallback;
    print
	"  </applet>\n";
}

sub writeButtonControl {
    my ($controlType, $group);
    if ($button =~ /push/i) {
	$controlType = "'chimePush'";
    } elsif ($button =~ /toggle/i) {
	$controlType = "'chimeToggle'";
    } elsif ($button =~ /radio(\d+)/i) {
	$controlType = "'chimeRadio'";
	$group = $1;
    }
    my $buttonScript = $script || $src;
    print
	"  <applet name=$name code=JmolAppletControl".
	" archive=JmolApplet.jar\n"
	if $name;
    print
	"  <applet code=JmolAppletControl archive=JmolApplet.jar\n"
	unless $name;
    print
	"          width=$width height=$height mayscript >\n";
    print
	"    <param name=target value=$target >\n".
	"    <param name=type   value=$controlType >\n";
    print
	"    <param name=group  value=$group >\n"
	if $group;
    print
	"    <param name=script value=$buttonScript >\n"
	if $buttonScript;
    print
	"    <param name=altscript value=$altscript >\n"
	if $altscript;
    print
	"    <param name=ButtonCallback value=$buttonCallback >\n"
	if $buttonCallback;
    print
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
