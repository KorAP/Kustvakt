package KorAP::VirtualCorpus::Or;
use strict;
use warnings;
use KorAP::VirtualCorpus::DocVec;
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
  return 'Or';
};


# Get operands
sub operands {
  shift->{ops};
};


# Flatten the group
sub flatten {
  my $self = shift;
  my %fields = ();
  my @ops;

  foreach (@{$self->{ops}}) {
    if ($_->koral_type eq 'Doc') {
      if ($_->type eq 'string' && $_->match eq 'eq') {
        $fields{$_->key} //= [];
        push @{$fields{$_->key}}, $_->value;
      };
    }
    elsif ($_->koral_type eq 'DocVec') {
      $fields{$_->key} //= [];
      push @{$fields{$_->key}}, @{$_->value};
    }
    elsif ($_->koral_type eq 'Or') {
      push @ops, @{$_->operands}
    }
    else {
      push @ops, $_->flatten;
    }
  };

  # Vectorize fields
  foreach (sort keys %fields) {
    push @ops, KorAP::VirtualCorpus::DocVec->new($_)->value(@{$fields{$_}});
  };

  if (@ops == 1) {
    return $ops[0]->name($self->name);
  };

  $self->{ops} = \@ops;
  return $self;
};


# Serialize to fragment
sub _to_fragment {
  my $self = shift;
  my $json = '{';
  $json .= '"@type":"koral:docGroup",';
  $json .= '"operation":"operation:or",';
  $json .= '"operands":[';
  $json .= join(',', map { $_->_to_fragment } @{$self->{ops}});
  $json .= ']';
  # Set at the end, when all comments are done
  $json .= $self->_commentparam_to_string;
  $json .= '}';
  return $json;
};


1;
