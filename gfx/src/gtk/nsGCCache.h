/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * The Original Code is the Mozilla browser.
 * 
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation. Portions created by Netscape are
 * Copyright (C) 1999 Netscape Communications Corporation. All
 * Rights Reserved.
 * 
 * Contributor(s): 
 *   Stuart Parmenter <pavlov@netscape.com>
 */

#include <gdk/gdk.h>
#include <string.h>

#define countof(x) (sizeof(x) / sizeof (*x))

struct GCData
{
  GdkGCValuesMask flags;
  GdkGCValues gcv;
  GdkRegion *clipRegion;
  GdkGC *gc;
};

class nsGCCache
{
 public:
  nsGCCache();
  virtual ~nsGCCache();


  void Flush(unsigned long flags);

  GdkGC *GetGCFromDW(GdkWindow *window, GdkGCValues *gcv, GdkGCValuesMask flags, GdkRegion *clipRegion);
  
  GdkGC *GetClipGC(GdkWindow *window, GdkGCValues *gcv, GdkGCValuesMask flags, GdkRegion *clipRegion);
  
  GdkGC *GetGC(GdkWindow *window, GdkGCValues *gcv, GdkGCValuesMask flags);

};
