# WDSS2 GUI Version 3.0 (Alpha)

Eclipse OSGI plugin based version of the java display from WG2.  I've been trying to cleanup/organize the java features of WG2 to go into an AWIPS2 model, and to remove dependencies.  I've been doing this 'top-down' which has been difficult due to code dependencies.  This will be a 'bottom-up' approach, which will be less functional for a bit, but more portable. 

  - WG2 uses worldwind and other stuff for rendering.  I need rendering to more generic (renderable to multiple display views).  We'll make a raw-opengl renderer with the ability to use other systems.
  - WG2 has lots of 'experiments' that depend upon multiple different libraries, such as XML reading/writing.
  - WG2 uses a 3rd party view system which needs to be replaced with the Eclipse windowing system.

> The overriding design goal for WG3 is to have one Eclipse E4 plugin to represent a standalone display system.  The other OSGI plugins will be stock java code for integrating directly into AWIPS2.