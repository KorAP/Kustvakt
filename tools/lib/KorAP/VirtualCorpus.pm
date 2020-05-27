package KorAP::VirtualCorpus;
use strict;
use warnings;


# Get or set name of the VC
sub name {
  my $self = shift;
  unless (@_) {
    return $self->{name};
  };
  $self->{name} = shift;
  return $self;
};


# Comment
sub comment {
  my $self = shift;
  unless (@_) {
    return $self->{comment};
  };
  $self->{comment} //= [];

  push @{$self->{comment}}, @_;
  return $self;
};


# Flatten the object - can be overwritten
sub flatten {
  shift;
};


# Serialize to koral object - can be overwritten
sub to_koral {
  shift;
};


# Quote utility function
sub quote {
  shift;
  my $str = shift;
  $str =~ s/(["\\])/\\$1/g;
  return qq{"$str"};
};


# Escaped quote utility function
sub equote {
  shift;
  my $str = shift;
  $str =~ s/(["\\])/\\$1/g;
  $str =~ s/(["\\])/\\$1/g;
  return '\\"' . $str . '\\"';
};


sub _commentparam_to_string {
  my $self = shift;
  my $comment = $self->_comment_to_string;
  if ($comment) {
    return qq!,"comment":"$comment"!;
  };
  return '';
};


sub _comment_to_string {
  my $self = shift;
  if (!$self->name && !$self->comment) {
    return '';
  };

  my $json = '';
  $json .= 'name:' . $self->equote($self->name) if $self->name;
  if ($self->name && $self->comment) {
    $json .= ','
  };
  $json .= join(',', @{$self->{comment}}) if $self->{comment};

  return $json;
};


# Stringify collection
sub to_string {
  my $self = shift;
  ## Create collection object

  my $obj = $self->to_koral;

  my $json = '{';
  $json .= '"@context":"http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",';
  $json .= '"collection":';
  $json .= $obj->_to_fragment;
  # Set at the end, when all comments are done
  $json .= $obj->_commentparam_to_string;
  return $json .= '}';
};

1;
