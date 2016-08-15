package com.google.j2cl.transpiler.integration.enums;

public class Main {

  static enum Foo {
    FOO,
    FOZ
  }

  static enum Bar {
    BAR(1),
    BAZ(Foo.FOO),
    BANG(5) {
      int getF() {
        return f + 2;
      }
    };

    static Bar[] ENUM_SET = {Bar.BAR, Bar.BAZ, Bar.BANG};

    int f;

    Bar(int i) {
      f = i;
    }

    Bar(Foo f) {
      this(f.ordinal());
    }

    int getF() {
      return f;
    }
  }

  public static void main(String[] args) {
    assert Foo.FOO.ordinal() == 0;
    assert Foo.FOO.name().equals("FOO");

    assert Foo.FOZ.ordinal() == 1;
    assert Foo.FOZ.name().equals("FOZ");

    assert Bar.BAR.ordinal() == 0;
    assert Bar.BAR.getF() == 1;
    assert Bar.BAR.name().equals("BAR");

    assert Bar.BAZ.ordinal() == 1;
    assert Bar.BAZ.getF() == 0;
    assert Bar.BAZ.name().equals("BAZ");

    assert Bar.BANG.ordinal() == 2;
    assert Bar.BANG.getF() == 7;
    assert Bar.BANG.name().equals("BANG");
    // Check use-before-def assigning undefined
    // Although it isn't likely the test will make it this far
    for (Bar b : Bar.ENUM_SET) {
      assert b != null;
    }
  }
}
