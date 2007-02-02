package tMysqlOutput::Mysql;

use Carp;

sub getTableCreationQuery {
    my %param = @_;

    my %talendtype_to_dbtype = (
        char    => 'CHAR',
        Day     => 'DATETIME',
        double  => 'DOUBLE',
        float   => 'FLOAT',
        int     => 'INT',
        long    => 'LONG',
        String  => 'VARCHAR',
    );

    # In $param{schema}, each column looks like this:
    #
    # {
    #     name    => 'shop_code',
    #     key     => true,
    #     type    => 'int',
    #     len     => null,
    #     precision => null,
    #     null    => false,
    #     default => '',
    #     comment => '',
    # }

    my $query;
    my $column_num = 1;
    my @key_names = map { $_->{name} } grep { $_->{key} } @{ $param{schema} };

    # MySQL creation table statement example
    #
    # CREATE TABLE `sales_copy` (
    #   `shop_code` int(11) NOT NULL,
    #   `ean` char(13) NOT NULL,
    #   `sales` int(11) default NULL,
    #   `quantity` int(11) default NULL,
    #   primary key(shop_code, ean)
    # );
    $query = 'CREATE TABLE `'.$param{tablename}.'` ('."\n";

    foreach my $column_href (@{ $param{schema} }) {
        $column_href->{dbtype} = $talendtype_to_dbtype{$column_href->{type}};

        if (lc $column_href->{type} eq 'string') {
            if (not defined $column_href->{len}
                or $column_href->{len} == -1) {
                $column_href->{len} = 255;
            }
        }

        if ($column_num++ > 1) {
            $query.= ', ';
        }

        $query.= '`'.$column_href->{name}.'`';
        $query.= ' '.$column_href->{dbtype};

        if (defined $column_href->{len} and $column_href->{len} != -1) {
            $query.= '(';
            $query.= $column_href->{len};

            if (grep /^$column_href->{type}$/, qw/float double/) {
                # REAL, DOUBLE, FLOAT, DECIMAL, NUMERIC
                $query.= ','.$column_href->{precision};
            }

            $query.= ')';
        }

        if (not $column_href->{null}) {
            $query.= ' NOT NULL';
        }

        if ($column_href->{default} != '') {
            $query.= " DEFAULT '".$column_href->{default}."'";
        }

        if ($column_href->{comment} != '') {
            $query.= sprintf(" COMMENT '%s'", $column_href->{comment});
        }

        $query.= "\n";

        $column_num++;
    }

    if (@key_names) {
        $query.= sprintf(
            ", PRIMARY KEY(%s)\n",
            join(
                ',',
                @key_names
            )
        );
    }

    $query.= ')';

#     use Data::Dumper;
#     print Dumper($param{schema});
#     print $query; exit();

    return $query;
}

1;
