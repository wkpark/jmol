#!/usr/bin/perl
#
# Copyright 2003 Cutthroat Communications, Inc. All rights reserved
#
use English;
use CGI;
use CGI::Carp;
use LWP;
use LWP::UserAgent;
use strict;

sub err;

my $cgi = new CGI;
my $url = $cgi->param('url');
if ($url) {
    my $userAgent = LWP::UserAgent->new;
    $userAgent->agent("JmolAppletProxy/1.0");

    my $request = HTTP::Request->new(GET => $url);
    my $response = $userAgent->request($request);
    if ($response->is_success()) {
	print $cgi->header(-type=>'text/plain'), $response->content();
	exit();
    }
}
print $cgi->header(-type=>'text/plain' -status=>'404 Not Found');
