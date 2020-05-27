package KorAP::VirtualCorpus::Group;
use strict;
use warnings;
use base 'KorAP::VirtualCorpus';
use KorAP::VirtualCorpus::Doc;
use KorAP::VirtualCorpus::And;
use KorAP::VirtualCorpus::Or;

# Abstract KoralQuery object that normalize to And or Or.

# Construct a new VC group
sub new {
  my $class = shift;
  bless {
    # New try
    ops => undef,
    type => undef
  }, $class;
};


# Clone object
sub clone {
  my $self = shift;
  my $clone = {};
  $clone->{ops} = [@{$self->{ops}}] if $self->{ops};
  $clone->{type} = $self->{type};
  $clone->{name} = $self->{name};
  bless $clone, __PACKAGE__;
};


# Add operand
sub add {
  my ($self, $type, $op) = @_;

  if (!$self->{ops}) {
    push @{$self->{ops}}, $op;
    return $self;
  };

  if (!$self->{type}) {
    push @{$self->{ops}}, $op;
    $self->{type} = $type;
    return $self;
  };

  if ($self->{type} eq $type) {
    push @{$self->{ops}}, $op;
    return $self;
  };

  if ($self->{type} eq 'union') {
    my $vc = KorAP::VirtualCorpus::Or->new(
      @{$self->{ops}}
    );

    $self->{type} = 'joint';
    $self->{ops} = [$vc, $op];
    return $self;
  };

  my $vc = KorAP::VirtualCorpus::And->new(
    @{$self->{ops}}
  );

  $self->{type} = 'union';
  $self->{ops} = [$vc, $op];
  return $self;
};


# Serialize to koral
sub to_koral {
  my $self = shift;

  # Single object
  if (@{$self->{ops}} == 1) {
    return $self->{ops}->[0]->name($self->name)->flatten;
  };

  # Union group
  if ($self->{type} eq 'union') {
    return KorAP::VirtualCorpus::Or->new(
      @{$self->{ops}}
    )->name($self->name)->flatten;
  }

  # Joint group
  elsif ($self->{type} eq 'joint') {
    return KorAP::VirtualCorpus::And->new(
      @{$self->{ops}}
    )->name($self->name)->flatten;
  };
};


# Define an operand to be "or"ed
sub union {
  my $self = shift;
  $self->add('union', shift)
};


# Define a field that should be "or"ed
sub union_field {
  my $self = shift;
  my $field = shift;
  my $value = shift;
  $self->union(
    KorAP::VirtualCorpus::Doc->new($field)->value($value)
  );
};

# Define an operand to be "and"ed
sub joint {
  my $self = shift;
  $self->add('joint', shift)
};


# Define a field that should be "and"ed
sub joint_field {
  my $self = shift;
  my $field = shift;
  my $value = shift;
  $self->joint(
    KorAP::VirtualCorpus::Doc->new($field)->value($value)
    );
};


# Restrict VC by date
sub from {
  my $self = shift;
  my ($year, $month) = @_;
  my $doc = KorAP::VirtualCorpus::Doc->new('createDate')
    ->type('date')
    ->match('geq')
    ->value($year . '-' . $month);
  $self->joint($doc);
};


# Restrict VC by date
sub to {
  my $self = shift;
  my ($year, $month) = @_;
  my $doc = KorAP::VirtualCorpus::Doc->new('createDate')
    ->type('date')
    ->match('leq')
    ->value($year . '-' . $month);
  $self->joint($doc);
};


1;

__END__
