package KorAP::VirtualCorpus::And;
use strict;
use warnings;
use base 'KorAP::VirtualCorpus';


# TODO:
#   Support comments!


# Constructor
sub new {
  my $class = shift;
  bless { ops => [@_] }, $class;
};


# Get koral type
sub koral_type {
  return 'And';
};


# Get operands
sub operands {
  shift->{ops};
};


# Flatten group
sub flatten {
  my $self = shift;

  my @ops;

  foreach (@{$self->{ops}}) {
    if ($_->koral_type eq 'And') {
      push @ops, @{$_->operands};
    }

    else {
      push @ops, $_->flatten;
    };
  };

  $self->{ops} = \@ops;

  return $self;
};


# Serialize fragment
sub _to_fragment {
  my $self = shift;
  my $json = '{';
  $json .= '"@type":"koral:docGroup",';
  $json .= '"operation":"operation:and",';
  $json .= '"operands":[';
  $json .= join(',', map { $_->_to_fragment } @{$self->{ops}});
  $json .= ']';

  # Set at the end, when all comments are done
  $json .= $self->_commentparam_to_string;
  $json .= '}';
  return $json;
};


1;
