# mir-mets-printer

`mir-mets-printer` is an application JAR module for use with MIR.

The module enables the MyCoRe Viewer print functionality and allows configuration of the maximum number of pages a user can print.

## Configuration

Set the `MIR.PDF.MAXPages` property to define the maximum number of printable pages:

```properties
MIR.PDF.MAXPages=500
```

The default value is `500`.

## Requirements

* MIR
* MyCoRe Viewer
