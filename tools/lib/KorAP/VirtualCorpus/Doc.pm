package KorAP::VirtualCorpus::Doc;
use strict;
use warnings;
use base 'KorAP::VirtualCorpus';

# Constructor
sub new {
  my $class = shift;
  bless {
    key => shift,
    match => 'eq',
    type => 'string',
    value => ''
  }, $class;
};


# Clone document VC
sub clone {
  my $self = shift;
  bless {
    key =>   $self->{key},
    match => $self->{match},
    type =>  $self->{type},
    value => $self->{value},
  }, __PACKAGE__;
};


# Return object type
sub koral_type {
  return 'Doc';
};


# Get or set type
sub type {
  my $self = shift;
  if (@_) {
    $self->{type} = shift;
    return $self;
  };
  return $self->{type};
};


# Get or set match
sub match {
  my $self = shift;
  if (@_) {
    $self->{match} = shift;
    return $self;
  };
  return $self->{match};
};


# Get or set key
sub key {
  my $self = shift;
  if (@_) {
    $self->{key} = shift;
    return $self;
  };
  return $self->{key};
};


# Get or set value
sub value {
  my $self = shift;
  if (@_) {
    $self->{value} = shift;
    return $self;
  };
  return $self->{value};
};


# Stringify fragment
sub _to_fragment {
  my $self = shift;
  my $json = '{';
  $json .= '"@type":"koral:doc",';
  $json .= '"type":"type:' . $self->type . '",';
  $json .= '"match":"match:' . $self->match . '",';
  $json .= '"key":"' . $self->key . '",';
  $json .= '"value":' . $self->quote($self->value);

  # Set at the end, when all comments are done
  $json .= $self->_commentparam_to_string;
  return $json . '}';
};


1;
