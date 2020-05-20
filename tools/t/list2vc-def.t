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
done_testing;
