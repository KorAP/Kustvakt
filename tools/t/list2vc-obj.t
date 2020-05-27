#!/usr/bin/env perl
use strict;
use warnings;
use Test::More;
use Data::Dumper;
use Mojo::JSON 'decode_json';

require_ok 'KorAP::VirtualCorpus::Group';

my $vc = KorAP::VirtualCorpus::Group->new;
$vc->union_field('author', 'Goethe');
$vc->union_field('author', 'Schiller');
$vc->joint_field('author', 'Fontane');


my $json = decode_json $vc->to_koral->to_string;

is($json->{collection}->{operation}, 'operation:and');
is($json->{collection}->{operands}->[0]->{'@type'}, 'koral:doc');
is($json->{collection}->{operands}->[0]->{'value'}->[0], 'Goethe');
is($json->{collection}->{operands}->[0]->{'value'}->[1], 'Schiller');

done_testing;
