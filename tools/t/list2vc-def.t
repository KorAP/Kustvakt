#!/usr/bin/env perl
use strict;
use warnings;
use Test::More;
use File::Basename;
use File::Spec::Functions;

use Test::Output;
use Mojo::JSON 'decode_json';

my $script = catfile(dirname(__FILE__), '..', 'list2vc.pl');
my $list1 = catfile(dirname(__FILE__), 'data', 'list2.def');

# Check STDOUT
stdout_like(
  sub {
    system($script, $list1);
  },
  qr!^\{\"\@context\".+?\}$!,
  "check stdout"
);

# Check JSON
my $json = decode_json(join('', `$script $list1`));

is($json->{'collection'}->{'@type'}, 'koral:docGroup', 'type');
is($json->{'collection'}->{'operation'}, 'operation:or', 'operation');

my $op1 = $json->{'collection'}->{'operands'}->[0];
is($op1->{'@type'}, 'koral:doc', 'type');
is($op1->{'key'}, 'docSigle', 'key');
is($op1->{'match'}, 'match:eq', 'match');
is($op1->{'value'}->[0], "BRZ05/SEP", 'value');
is($op1->{'value'}->[1], ,"BRZ05/OKT", 'value');
is($op1->{'value'}->[-1], ,"BRZ08/FEB", 'value');

my $op2 = $json->{'collection'}->{'operands'}->[1];
is($op2->{'@type'}, 'koral:doc', 'type');
is($op2->{'key'}, 'textSigle', 'key');
is($op2->{'match'}, 'match:eq', 'match');
is($op2->{'value'}->[0], "B19/AUG/01665", 'value');
is($op2->{'value'}->[1], ,"B19/AUG/01666", 'value');


my $list2 = catfile(dirname(__FILE__), 'data', 'list3.def');

# Check JSON
# Only return extended area
$json = decode_json(join('', `$script $list2`));

is($json->{'collection'}->{'@type'}, 'koral:docGroup', 'type');
is($json->{'collection'}->{'operation'}, 'operation:or', 'operation');
is($json->{'collection'}->{'comment'}, 'Name: "VAS-N91 (Stand \"2013\", korr. 2017)"', 'type');


$op1 = $json->{'collection'}->{'operands'}->[0];
is($op1->{'@type'}, 'koral:doc', 'type');
is($op1->{'key'}, 'textSigle', 'key');
is($op1->{'match'}, 'match:eq', 'match');
is($op1->{'value'}->[0], "A00/APR/23232", 'value');
is($op1->{'value'}->[1], ,"A00/APR/23233", 'value');

done_testing;
