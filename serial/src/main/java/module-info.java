
module io.smallrye.common.serial {
    requires io.smallrye.common.constraint;
    requires jdk.unsupported;

    exports io.smallrye.common.serial;
    exports io.smallrye.common.serial.spi;

    uses io.smallrye.common.serial.spi.ObjectSerializer;
    uses io.smallrye.common.serial.spi.ObjectDeserializer;
}
