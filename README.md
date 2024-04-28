# The Adumbra library
A light-weight Java library that uses
[steganography](https://en.wikipedia.org/wiki/Steganography)
to hide data in bitmaps using a secret key. 
Supported input formats include PNG, JPEG, TIFF, BMP.
Output formats are PNG and TIFF only, because other
formats are lossy and therefore more difficult to support.

Adumbra requires a bitmap with about 500 pixels
for every byte of the secret message, so a bitmap of
500x700 pixels could contain a secret message 
of up to about 700 bytes. The maximum size of the secret
message is 32K bytes.

Adumbra is intended to be used as more of a way
to mark bitmaps in a non-obvious way, rather than as 
an efficient way to transmit large amounts of secret data.

## How does it work?
Adumbra hides a secret message in a bitmap by distributing
the message's bits into the least significant bit of some pixels,
using one color per pixel (R, G or B) in a pattern
determined by the secret key.

Before being encoded into the bitmap, the message is 
encrypted using a secure hash of the secret key, 
and the bits are distributed into the bitmap using 
the hash.

Adumbra also randomizes all the least significant bits 
of other pixels to make it more difficult to determine 
whether the bitmap contains a secret message, and 
how long that message may be.

Therefore, even if someone had access to the original bitmap,
they still would not be able to decode the secret message
without the secret key because they could not determine
if a changed bit belongs to the secret message or is just noise.

## How secure is it?
This library has not been reviewed by cryptography experts,
so you should exercise common sense -- do not rely on it
to hide national security secrets.

As always with encryption, the length of the secret key is
important: using a short key is less secure than a longer key.
The randomness of the key is also a factor. In general,
using a secret key of at least 40 random characters should
give you excellent secrecy. If you just want to mark a bitmap in a non-obvious
way without worrying about NSA-level adversaries, 15 or 20
random characters should be plenty.

There are two main obstacles to decoding a secret message:
### Determining that a bitmap contains a message
An attacker who analyzes the modified bitmap may be able to determine
that the bitmap contains some suspicious noise, but it would be
difficult to be certain unless they have access to the original
bitmap.
### Decoding the secret message
Even with the original bitmap, without the secret key, 
an attacker would have a difficult time to
figure out which bits actually belong to the secret message.
Even if they did, they would then have to decrypt the message,
which is encoded using a SHA-512 hash of the secret key.

## Command line usage
### Encoding a message in a bitmap
```
java -jar adumbra-<version>.jar encode \
    <input-file> \
    <output-file> \
    <message> \
    <key> \
    [<format> [<secure-level>]]
```
- `<input-file>`: a bitmap file. 
Supported input formats are those supported by your Java platform,
and usually include: `png`, `jpeg`, `tiff`, `bmp`
- `<output-file>`: the file in which to write the bitmap 
with the hidden message
- `<message>`: the message to be encoded in the bitmap
- `<key>`: the secret key used to hide the message
- `<format>`: optional, the format of the output file. 
If not specified, the format of the input file will be used,
but only PNG and TIFF are allowed because other formats
are lossy.
- `<secure-level>`: optional, an integer between 0 and 2.
Zero means minimum security, no noise is added to the image,
which means, depending on the bitmap, it may be easy to
detect that the image contains secret data. One adds some
noise, but with some repetition. Two adds fully random noise,
but is slower.

### Example
Encode a message into a bitmap file:
```
java -jar adumbra-0.8.jar encode MyImage.jpeg Output.png \
    "My secret message" "My secret key" png
```
## Extract a secret message from a bitmap file:
```
java -jar adumbra-0.8.jar decode Output.png \
"My secret key"
```
Output:
```
Hidden message: My secret message
```

## Library usage
This is a stand-alone library, it has no dependencies.

### Maven:
```
<dependency>
    <groupId>com.galliumdata.adumbra</groupId>
    <artifactId>adumbra</artifactId>
    <version>0.8</version>
</dependency>
```
### Encoding in Java
```
Encoder encoder = new Encoder();
FileInputStream inStr = new FileInputStream("MyImage.jpg"");
FileOutputStream outStr = new FileOutputStream("ModifImage.png");
byte[] message = "This is the message".getBytes(StandardCharsets.UTF_8);
String key = "This is the secret key";
encoder.encode(inStr, outStr,  "png", message, key);
outStr.close();
// Result is in file ModifImage.png
```

### Decoding in Java
```
Decoder decoder = new Decoder();
FileInputStream inStr = new FileInputStream("ModifImage.png");
byte[] decoded = decoder.decode(inStr, "This is the secret key");
System.out.println(new String(decoded));
```

# About the author
Adumbra was developed while working on 
[Gallium Data](https://www.galliumdata.com)
to allow invisible watermarking of bitmaps stored in databases.
It is open source with an Apache 2.0 license.
