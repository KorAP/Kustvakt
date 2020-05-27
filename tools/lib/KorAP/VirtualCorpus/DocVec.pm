package KorAP::VirtualCorpus::DocVec;
use strict;
use warnings;
use base 'KorAP::VirtualCorpus::Doc';

# Constructor
sub new {
  my $class = shift;
  bless {
    key => shift,
    match => 'eq',
    type => 'string',
    value => []
  }, $class;
};

# Return object type
sub koral_type {
  return 'DocVec';
};

# Get or set value
sub value {
  my $self = shift;
  if (@_) {
    $self->{value} = [@_];
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
  $json .= '"value":[' . join(',', map { $self->quote($_) } @{$self->value}) . ']';

  # Set at the end, when all comments are done
  $json .= $self->_commentparam_to_string;
  return $json . '}';
};


1;
