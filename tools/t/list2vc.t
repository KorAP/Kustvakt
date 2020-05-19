#!/usr/bin/env perl
use strict;
use warnings;
use Test::More;
use File::Basename;
use File::Spec::Functions;

use Test::Output;
use Mojo::JSON 'decode_json';

my $script = catfile(dirname(__FILE__), '..', 'list2vc.pl');
my $list1 = catfile(dirname(__FILE__), 'data', 'list1.txt');

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
is($op1->{'key'}, 'corpusSigle', 'key');
is($op1->{'match'}, 'match:eq', 'match');
is_deeply($op1->{'value'}, ["A02","A03"], 'value');

my $op2 = $json->{'collection'}->{'operands'}->[1];
is($op2->{'@type'}, 'koral:doc', 'type');
is($op2->{'key'}, 'docSigle', 'key');
is($op2->{'match'}, 'match:eq', 'match');
is_deeply($op2->{'value'}, ["B04/X02","B04/X03"], 'value');

my $op3 = $json->{'collection'}->{'operands'}->[2];
is($op3->{'@type'}, 'koral:doc', 'type');
is($op3->{'key'}, 'textSigle', 'key');
is($op3->{'match'}, 'match:eq', 'match');
is_deeply($op3->{'value'}, ["A01/B02/c04","A01/B02/c05"], 'value');


# Check STDIN
my $json2 = decode_json(join('', `cat $list1 | $script -`));
is_deeply($json, $json2);

done_testing;
