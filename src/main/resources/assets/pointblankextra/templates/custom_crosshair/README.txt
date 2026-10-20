Custom Crosshair PSD templates
==============================

These are editable source files for content-pack creators. Minecraft does not
load PSD files; export a PNG when finished.

Files
-----

custom_crosshair_native_39.psd
    Exact final-size native atlas for "layout": "pointblank".
    Canvas: 39x39.
    Layers: Top Segment, Bottom Segment, Left Segment, Right Segment, Center,
            Guides / Safe Areas.
    The Guides layer is hidden by default; keep it hidden when exporting.

custom_crosshair_native_workbook_312.psd
    8x working version of the same native atlas.
    Canvas: 312x312.
    Edit at this size, hide the Guides layer, then resize/export to exactly
    39x39 (8:1) for "layout": "pointblank".

custom_crosshair_complete_512.psd
    Workbook for the default static complete-PNG layout.
    Canvas: 512x512.
    Layers: Artwork, Guides (hidden).
    Export any PNG size. In the weapon JSON, "size" controls the largest
    rendered HUD dimension. This layout is static; it does not use VPB's
    segmented dynamic crosshair behavior.

Native atlas regions (39x39)
----------------------------

Top segment    : x 16..22, y 0..15
Bottom segment : x 16..22, y 23..38
Left segment   : x 0..15, y 16..22
Right segment  : x 23..38, y 16..22
Center         : x 16..22, y 16..22

Keep all other pixels transparent, especially the corners. Point Blank's
native renderer samples the side regions at full height and the top/bottom
regions at full width; transparent padding prevents duplicate pieces.

Exporting
---------
1. Edit the segment layers.
2. Keep the Guides layer hidden.
3. Export a PNG with transparency.
4. For native layout, the final PNG must be exactly 39x39.
5. Use it with "layout": "pointblank".

For a static complete crosshair, export any size and omit "layout" (or use
"layout": "complete").
